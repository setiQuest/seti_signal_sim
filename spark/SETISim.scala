package org.seti.simulator

import org.apache.spark._
import java.util.Random;
import java.io._;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import apps.simulate.GaussianNoise;
import apps.simulate.FileNoise;
import apps.simulate.NoiseGenerator;
import apps.simulate.DataSimulator;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.util.Properties

import java.sql.ResultSet

// import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
// import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
// import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.collection.JavaConverters._

import com.fasterxml.jackson.databind.ObjectMapper;

import org.seti.simulator.errors.MisMatchDigest
import org.seti.simulator.errors.MissingSunNoise
import org.seti.simulator.utils.HexBytesUtil
import org.seti.simulator.objectstorage.OpenStack4jObjectStore
import org.seti.simulator.objectstorage.SwiftObjStore
import org.seti.simulator.database.DashDB
import org.seti.simulator.signaldef.SignalDefFactory
import org.seti.simulator.signaldef.SignalDef
//import org.seti.simulator.SunNoise

import com.ibm.ibmos2spark.bluemix

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.EOFException;
import java.io.IOException;
import java.net.URI;

import org.apache.hadoop.conf.Configuration

object SETISim {
  
  //will this parallelize properly within spark?
  //or should each job make a new ObjectStore and DashDB connection??
  //will they even be able to be used 
  var configurationName : String = "setipublic"
  //var dataClass : String = "" //this will either be 'training', 'test', 'basic', or 'private'



  // def makeNoiseGen(noiseName: String, seed: Long, sigdef: SignalDef) : NoiseGenerator =  {
  //   if (noiseName == "gaussian") {
  //     var noiseGen = new GaussianNoise(seed);
  //     noiseGen.setAmp(sigdef.sigmaN);
  //     return noiseGen
  //   } else if (noisename == "sunnoise") {
  //     //pop 
  //   } else {
  //     return new FileNoise(noiseName);
  //   }
  // }


  def sparkSim(numPartitions: Int, nSim: Int, paramGenName: String, noiseName: String, dataClass: String) {
    //paramGenName is the same as the signal class!

    val conf = new SparkConf().setAppName("SETI Sim")
    val sc = new SparkContext(conf)

    //prevents creating empty partitions, which can result in a pointer exception
    //when iterating through data (such as during mapPartitionsWithIndex function)
    var numPartitionsNeeded = numPartitions;

    if (nSim < numPartitionsNeeded) {
      numPartitionsNeeded = nSim;
    }

    val initSeed: Long = System.currentTimeMillis()*nSim*numPartitionsNeeded

    //needed for the SwiftObjStore
    val props = new Properties
    props.load(getClass.getResourceAsStream("/simulation.properties"))
    var dashdb_database_name = props.getProperty("databasename")
    
    var simulatedSignalContainer : String = ""  
    
    dataClass match {
      case "test" => {
        simulatedSignalContainer = props.getProperty("test_data_container")
        dashdb_database_name = props.getProperty("test_databasename")
      }
      case "basic" => {
        simulatedSignalContainer = props.getProperty("basic_data_container")
      }
      case "basictest" => {
        simulatedSignalContainer = props.getProperty("basic_test_data_container")
        dashdb_database_name = props.getProperty("basic_test_databasename")
      }
      case "training" => {
        simulatedSignalContainer = props.getProperty("training_data_container")
      }
      case "private" => {
        simulatedSignalContainer = props.getProperty("private_data_container")
      }
      case _ => {
        println("Incorrect data class ($dataClass). Choose either 'test', 'training', 'basic', 'basictest' or 'private'.")
        return
      }
    }

    println(s"Found $props")
    
    println("Generating initial RDD")
    var noiseArray : Array[(Int, String, String, String)] = new Array[(Int, String, String, String)](nSim)
    
    println("Noise Array of size: " + noiseArray.length)


    var rand = new Random(initSeed)

    if (noiseName == "sunnoise") {
      //need to get list of nSim noise files from database
      //build new rdd with (i, container, objectname)
      println("querying database for sun noise")
      val dashdbForSun : DashDB = new DashDB(props.getProperty("JDBC_URL"), props.getProperty("DASHDBUSER"), props.getProperty("DASHDBPASS"), dashdb_database_name)  //will need to use a connection pool. 
      var sunnoise = dashdbForSun.get_sun_noise(nSim)
      var counter: Int = 0
      println("filling sun noise")
      while ( sunnoise.next ) {

        if (rand.nextDouble > 0.0) {  //keep this fraction secret!  some number of the simulated noise files is gaussian!
          noiseArray(counter) = (counter, sunnoise.getString("container"), sunnoise.getString("objectname"), sunnoise.getString("uuid"))
        }
        else {
          noiseArray(counter) = (counter, "gaussian", "", "")
        }
        //print out the first first and last five to make sure
        if (counter < 5 || counter > nSim - 5) {
          println(noiseArray(counter).toString)
        }
        counter+=1
      }
      
      if (counter != nSim ) {
        throw MissingSunNoise(s"Found $counter noise files, expected $nSim")
      }
      dashdbForSun.connection.close()
    }
    else {
      //build new rdd with (i, noiseName, "", "")
      for (i <- 0 until nSim) {
        noiseArray(i) = (i, noiseName, "", "")
      }
    }

    var rdd = sc.parallelize(noiseArray, numPartitionsNeeded)
 

    var count = rdd.count
    println(s"Starting $count simulations... ")

    var rdd2 = rdd.mapPartitionsWithIndex { (indx, iter) =>

      //how do I bail if iter is empty? or if it's null?
      //if (!iter.hasNext) {
      //  return iter
      //}

      val dashDBConnection : DashDB = new DashDB(props.getProperty("JDBC_URL"), props.getProperty("DASHDBUSER"), props.getProperty("DASHDBPASS"), dashdb_database_name)  //will need to use a connection pool. 

      var seed:Long = initSeed + indx
      var randGen = new Random(seed)

      var mConf: Configuration = new Configuration(true)
      mConf.set("fs.swift2d.impl","com.ibm.stocator.fs.ObjectStoreFileSystem");
      mConf.set(s"fs.swift2d.service.$configurationName.auth.url", props.getProperty("auth_url") + "/auth/tokens");
      mConf.set(s"fs.swift2d.service.$configurationName.public", "true");
      mConf.set(s"fs.swift2d.service.$configurationName.tenant", props.getProperty("project_id"));
      mConf.set(s"fs.swift2d.service.$configurationName.password", props.getProperty("password"));
      mConf.set(s"fs.swift2d.service.$configurationName.username", props.getProperty("user_id"));
      mConf.set(s"fs.swift2d.service.$configurationName.region", props.getProperty("region"));
      mConf.set(s"fs.swift2d.service.$configurationName.region", props.getProperty("region"));

      var maxPerRoute:Int = 100
      if (nSim > 100) {
        maxPerRoute = nSim
      }
      var maxTotal:Int = 2*maxPerRoute

      mConf.set(s"fs.stocator.MaxPerRoute", maxPerRoute.toString)
      mConf.set(s"fs.stocator.MaxTotal", maxTotal.toString)
      mConf.set(s"fs.stocator.SoTimeout", "10000")
      mConf.set(s"fs.stocator.ReqConnectTimeout", "10000")
      mConf.set(s"fs.stocator.ReqConnectionRequestTimeout", "10000")
      mConf.set(s"fs.stocator.ReqSocketTimeout", "10000")
      
      val objstore: SwiftObjStore = new SwiftObjStore(mConf,configurationName)

      val myReturnIter = iter.map(i => {
          
        //
        // each row, i, is a tuple: (int, noisename, "", "") or (int, container, objectname, uuid)
        //

        //val objstore: SwiftObjStore = new SwiftObjStore(mConf,configurationName)
        //val objstore : OpenStack4jObjectStore = new OpenStack4jObjectStore(props, configurationName)
        // val dashdbSlow : DashDB = new DashDB(props.getProperty("JDBC_URL"), props.getProperty("DASHDBUSER"), props.getProperty("DASHDBPASS"),  dashdb_database_name)  //will need to use a connection pool. 

        var uuid:String = UUID.randomUUID().toString()
        var status = "success"
        var outputFileName = s"$uuid.dat"
        var message = s"starting simulation $seed for original $uuid\n"
        

        var sigdef = SignalDefFactory(paramGenName, randGen, dataClass)

        var noiseGen : NoiseGenerator = null
        if(i._2 == "gaussian") {
          noiseGen = new GaussianNoise(randGen);
          noiseGen.setAmp(sigdef.sigmaN);
        }
        else {
          if(i._3 == "") {
            noiseGen = new FileNoise(i._2)
            noiseGen.setAmp(1.0)
          }
          else {
              // noiseGen = new SunNoise(i._2, i._3, objstore) 
              // noiseGen.setAmp(1.0)
            try{
              noiseGen = new SunNoise(i._2, i._3, objstore) 
              noiseGen.setAmp(1.0)
            } catch {
              case e : Throwable => { 
                status = "failed"
                message += "row: " + i.toString + "\n"
                message += "SunNoise exception\n"
                message += s"${e.getMessage}\n"
              }
            }
          }
        }

        if (status == "success") {
          //val noiseGen = makeNoiseGen(noiseName, seed, sigdef)


          var DS = new DataSimulator(noiseGen, sigdef.sigmaN, sigdef.deltaPhiRad, sigdef.SNR, sigdef.drift, 
            sigdef.driftRateDerivate, sigdef.sigmaSquiggle, sigdef.outputLength, sigdef.ampModType, sigdef.ampModPeriod, 
            sigdef.ampModDuty, sigdef.signalClass, seed, randGen, uuid);

          var rawSimulatedDataByteStream = new ByteArrayOutputStream(sigdef.outputLength);

          DS.run(rawSimulatedDataByteStream);


          var dataOutputByteStream = new ByteArrayOutputStream(sigdef.outputLength);

          //only add the public header to output byte stream.
          var mapper = new ObjectMapper();
          val digest:MessageDigest = MessageDigest.getInstance("MD5");

          //var json = mapper.writeValueAsString(DS.labeledPublicHeader);
          //System.out.println(json);
          if (dataClass == "test" || dataClass == "basictest") {
            //use the unlabeled public header -- this JUST provides a UUID for the data file

            //also, for the TEST cases, we are going to change the UUID value to a MD5 hash in order to 
            //ensure test data cannot be grouped together by UUID, since UIID encodes the time stamp.
            //For example, if 100 test simulations were created at the same time, thier UUIDs could be used
            //to reconstruct the time they were created and then allow somebody to group them together
            var hashBytes = digest.digest(rawSimulatedDataByteStream.toByteArray());
            DS.uuid = HexBytesUtil.bytes2hex(hashBytes)
     
            DS.privateHeader.put("uuid", DS.uuid);
            DS.labeledPublicHeader.put("uuid", DS.uuid);
            DS.unlabeledPublicHeader.put("uuid", DS.uuid);

            dataOutputByteStream.write(mapper.writeValueAsBytes(DS.unlabeledPublicHeader));
          }
          else {
            dataOutputByteStream.write(mapper.writeValueAsBytes(DS.labeledPublicHeader));
          }
          dataOutputByteStream.write('\n');
          dataOutputByteStream.write(rawSimulatedDataByteStream.toByteArray());

         
          outputFileName = s"${DS.uuid}.dat"
          message += s"File name: $outputFileName\n"

          noiseGen.close();

          var hashBytes = digest.digest(dataOutputByteStream.toByteArray);
          var localEtag = HexBytesUtil.bytes2hex(hashBytes)
        
          //val dashdbSlow : DashDB = new DashDB(sys.env("JDBC_URL"), sys.env("DASHDBUSER"), sys.env("DASHDBPASS"))  //will need to use a connection pool. 
          
          
          //message += "Starting database transfer\n"

          try {
            noiseGen match {
              case m:SunNoise => {
                dashDBConnection.update_sun_noise_usage(i._4, "True")
                dashDBConnection.noise_file_uuid(i._4)
              }
              case _ => {
                dashDBConnection.noise_file_uuid("")
              }
            }

            
            dashDBConnection.uuid(DS.uuid)
            dashDBConnection.sigN(DS.sigN);
            dashDBConnection.noiseName(noiseGen.getName());
            dashDBConnection.dPhi(DS.dPhi);
            dashDBConnection.SNR(DS.SNR);
            dashDBConnection.drift(DS.drift);
            dashDBConnection.driftRateDerivative(DS.driftRateDerivate);
            dashDBConnection.jitter(DS.jitter);
            dashDBConnection.len(DS.len);
            dashDBConnection.ampModType(DS.ampModType);
            dashDBConnection.ampModPeriod(DS.ampModPeriod);
            dashDBConnection.ampModDuty(DS.ampModDuty);
            dashDBConnection.ampPhase(DS.ampPhase);
            dashDBConnection.ampPhaseSquare(DS.ampPhaseSquare);
            dashDBConnection.ampPhaseSine(DS.ampPhaseSine);
            dashDBConnection.signalClass(DS.signalClass);
            dashDBConnection.seed(DS.seed);
            dashDBConnection.mDriftDivisor(DS.mDriftDivisor);
            dashDBConnection.sinDrift(DS.sinDrift);
            dashDBConnection.cosDrift(DS.cosDrift);
            dashDBConnection.simulationVersion(DS.simulationVersion);
            dashDBConnection.simulationVersionDate(DS.simulationVersionDate);

            dashDBConnection.time(new Timestamp(System.currentTimeMillis()));
            dashDBConnection.container(simulatedSignalContainer);
            dashDBConnection.outputFileName(outputFileName);
            dashDBConnection.etag(localEtag);

            message += s"PUT to object store $simulatedSignalContainer, $outputFileName, SNR: ${DS.SNR}, class: ${DS.signalClass}, data_class: $dataClass\n"

            objstore.put(simulatedSignalContainer, outputFileName, dataOutputByteStream.toByteArray)
            
            
            // if(returnedEtag != localEtag {
            //   throw MisMatchDigest(s"$etag != $localEtag")
            // }
            // else {
            //   println("MD5 match okay")
            //   println("checksum algorith: " + fschecksum.getAlgorithmName)
            // }

            //update database
            message += s"INSERT to dashDB. etag: $localEtag\n"

            dashDBConnection.insertDataStatement.executeUpdate

          } catch {
              // ... code to handle exceptions
              // if DB connection exception, don't push data file to Object Storage. 
              //Need to print out info for logs. 
              case e : SQLException => {
                printException(e)
                message += s"SQLException\n"
                message += s"${e.getMessage}\n"
                status = "failed"
                try {
                  println("Transaction is being rolled back");
                  dashDBConnection.connection.rollback;
                  objstore.delete(simulatedSignalContainer, outputFileName);  
                } catch {
                  case ee : Throwable => {
                    ee.printStackTrace
                    message += s"Rollback/Delete Exception\n"
                    message += s"${ee.getMessage}\n"
                  }
                }
              }
              case e : Throwable => {
                e.printStackTrace
                status = "failed"
                message += s"General Exception\n"
                message += s"${e.getMessage}\n"
                try {
                  println("Transaction is being rolled back");
                  dashDBConnection.connection.rollback
                  objstore.delete(simulatedSignalContainer, outputFileName);
                } catch {
                  case ee : Throwable => {
                    ee.printStackTrace
                    message += s"Rollback/Delete Exception\n"
                    message += s"${ee.getMessage}\n"
                  }
                }
              }
          }  finally {
            message += "finally\n"
            //dashdbSlow.insertDataStatement.close
            // dashdbSlow.connection.close
            //objstore.fs.close
          }
        }
        
        println(s"$status")

        (seed, uuid, status, message, outputFileName, i._2)

      }).toList


      dashDBConnection.connection.close()
      objstore.fs.close()

      myReturnIter.iterator
    }

    //rdd2.count()
    //var rdd3 = rdd2.filter(i => {i._3 == "failed"})
    var results = rdd2.collect()

    //println("Completed " + rdd3.count() + " simulations out of " + nSim + " requested of type " +  paramGenName)
    //println("Failed " + rdd3.count() + " simulations out of " + nSim + " requested of type " +  paramGenName)
    

    println("Returned: " + results.length + " simulations out of " + nSim + " requested of type " +  paramGenName)
    results.foreach(i => {println(i._4)})

    println("Number of simulations by noise type.")
    //var noiseTypes = results.map(i => i._6)
    println(results.map(i => i._6).groupBy(identity).mapValues(_.size))


    var success = results.filter(i => {i._3 == "success"})
    println("Successful: " + success.length + " simulations out of " + nSim + " requested of type " +  paramGenName)
    success.foreach(i => {println("generated file: " + i._5)})

    var failures = results.filter(i => {i._3 == "failed"})
    println("Failed messages")
    failures.foreach(i => {println(i._4)})

    //println("Generated " + rdd6.count() + " simulations")
    sc.stop()
  }

  def serialSim(nSim: Int, paramGenName: String, noiseName: String, local: Boolean, dataClass: String) {

    val props = new Properties
    var simulatedSignalContainer : String = ""
    
    props.load(getClass.getResourceAsStream("/simulation.properties"))
    var dashdb_database_name = props.getProperty("databasename")

    dataClass match {
      case "test" => {
        simulatedSignalContainer = props.getProperty("test_data_container")
        dashdb_database_name = props.getProperty("test_databasename")
      }
      case "basic" => {
        simulatedSignalContainer = props.getProperty("basic_data_container")
      }
      case "basictest" => {
        simulatedSignalContainer = props.getProperty("basic_test_data_container")
        dashdb_database_name = props.getProperty("basic_test_databasename")
      }
      case "training" => {
        simulatedSignalContainer = props.getProperty("training_data_container")
      }
      case "private" => {
        simulatedSignalContainer = props.getProperty("private_data_container")
      }
      case _ => {
        println("Incorrect data class ($dataClass). Choose either 'test', 'training', 'basic', 'basictest' or 'private'..")
        return
      }
    }

    val objstore : OpenStack4jObjectStore = new OpenStack4jObjectStore(props, configurationName)
    val dashdb: DashDB = new DashDB(props.getProperty("JDBC_URL"), props.getProperty("DASHDBUSER"), props.getProperty("DASHDBPASS"), dashdb_database_name)  
    //val paramGen:ParameterGenerator = new ParameterGenerator(paramGenName)
    
    val seed: Long = System.currentTimeMillis()
    var randGen = new Random(seed)

    for (i <- 0 until nSim) {
      
      var sigdef = SignalDefFactory(paramGenName, randGen, dataClass)
      
      val digest:MessageDigest = MessageDigest.getInstance("MD5");  //I could probably do this outside of the loop and call reset, but I don't want to test this right now.

      var noiseGen : NoiseGenerator = null
      if(noiseName == "gaussian") {
        noiseGen = new GaussianNoise(randGen)
        noiseGen.setAmp(sigdef.sigmaN)
      }  //todo -- need to handle sun noise here... maybe. probably won't ever run this in serial mode anyways. serial mode is primarily for testing
      else {
        noiseGen = new FileNoise(noiseName)
        noiseGen.setAmp(1.0)
      }
      //val noiseGen = makeNoiseGen(noiseName, seed, sigdef)

      var uuid:String = UUID.randomUUID().toString()
      println(s"original uuid: $uuid")
      println(sigdef.toString)
      

      var DS = new DataSimulator(noiseGen, sigdef.sigmaN, sigdef.deltaPhiRad, sigdef.SNR, sigdef.drift, 
        sigdef.driftRateDerivate, sigdef.sigmaSquiggle, sigdef.outputLength, sigdef.ampModType, sigdef.ampModPeriod, 
        sigdef.ampModDuty, sigdef.signalClass, seed, randGen, uuid);
    
      // var dataOutputByteStream = new ByteArrayOutputStream(sigdef.outputLength);
      var rawSimulatedDataByteStream = new ByteArrayOutputStream(sigdef.outputLength);

      DS.run(rawSimulatedDataByteStream)

      //check to see how many times the simulated amplitude was beyond the 
      //8-bit range
      println("Number of X pol amplitude beyond dynamic range " + DS.numBeyondDynamicRangeX)
      println(f"                ${DS.numBeyondDynamicRangeX} / ${DS.len} : ${100*DS.numBeyondDynamicRangeX / DS.len.toFloat}%.2f %%")
      println("Number of Y pol amplitude beyond dynamic range " + DS.numBeyondDynamicRangeY)
      println(f"                ${DS.numBeyondDynamicRangeY} / ${DS.len} : ${100*DS.numBeyondDynamicRangeY / DS.len.toFloat}%.2f %%")

      // if (DS.numBeyondDynamicRangeX > 0.5*DS.len || DS.numBeyondDynamicRangeY > 0.5*DS.len) {
      //   println("try a smaller amplitude. good bye");
      //   return;
      // }

      //only add the public header to output byte stream.

      var mapper = new ObjectMapper();
      var dataOutputByteStream = new ByteArrayOutputStream(sigdef.outputLength);

      if (dataClass == "test" || dataClass == "basictest") {
        //use the unlabeled public header -- this JUST provides a UUID for the data file

        //also, for the TEST cases, we are going to change the UUID value to a MD5 hash in order to 
        //ensure test data cannot be grouped together by UUID, since UIID encodes the time stamp.
        //For example, if 100 test simulations were created at the same time, thier UUIDs could be used
        //to reconstruct the time they were created and then allow somebody to group them together
        var hashBytes = digest.digest(rawSimulatedDataByteStream.toByteArray());
        DS.uuid = HexBytesUtil.bytes2hex(hashBytes)
 
        DS.privateHeader.put("uuid", DS.uuid);
        DS.labeledPublicHeader.put("uuid", DS.uuid);
        DS.unlabeledPublicHeader.put("uuid", DS.uuid);

        dataOutputByteStream.write(mapper.writeValueAsBytes(DS.unlabeledPublicHeader));
      }
      else {
        dataOutputByteStream.write(mapper.writeValueAsBytes(DS.labeledPublicHeader));
      }
      dataOutputByteStream.write('\n');
      dataOutputByteStream.write(rawSimulatedDataByteStream.toByteArray());

      //  output file name is based on uuid
      var outputFileName = s"${DS.uuid}.dat"
      println("file: " + outputFileName + "\n")
      println("labeled public header: \n")
      System.out.println( mapper.writeValueAsString(DS.labeledPublicHeader) );


      try {
            
        // noiseGen match {
        //   case m:SunNoise => {
        //     dashdb.update_sun_noise_usage(i._4, "True")
        //     dashdb.noise_file_uuid(i._4)
        //   }
        //   case _ => {
        //     dashdb.noise_file_uuid("")
        //   }
        // }
        if (!local) {
          dashdb.noise_file_uuid("")
          
          dashdb.uuid(DS.uuid)
          dashdb.sigN(DS.sigN);
          dashdb.noiseName(noiseGen.getName());
          dashdb.dPhi(DS.dPhi);
          dashdb.SNR(DS.SNR);
          dashdb.drift(DS.drift);
          dashdb.driftRateDerivative(DS.driftRateDerivate);
          dashdb.jitter(DS.jitter);
          dashdb.len(DS.len);
          dashdb.ampModType(DS.ampModType);
          dashdb.ampModPeriod(DS.ampModPeriod);
          dashdb.ampModDuty(DS.ampModDuty);
          dashdb.ampPhase(DS.ampPhase);
          dashdb.ampPhaseSquare(DS.ampPhaseSquare);
          dashdb.ampPhaseSine(DS.ampPhaseSine);
          dashdb.signalClass(DS.signalClass);
          dashdb.seed(DS.seed);
          dashdb.mDriftDivisor(DS.mDriftDivisor);
          dashdb.sinDrift(DS.sinDrift);
          dashdb.cosDrift(DS.cosDrift);
          dashdb.simulationVersion(DS.simulationVersion);
          dashdb.simulationVersionDate(DS.simulationVersionDate);

          dashdb.time(new Timestamp(System.currentTimeMillis()));
          dashdb.container(simulatedSignalContainer);
          dashdb.outputFileName(outputFileName);
        } 

        

        
        //first, insert data file to object store
        //then verify it is there (should I do a md5 checksum?)

        //then, insert into to dashDB database
        //if that fails, need to remove data form object store as well as 
        //rollback dashdb transaction

        //upload output file
        var dataBytes = dataOutputByteStream.toByteArray();
         //calculate local md5
        
        var hashBytes = digest.digest(dataBytes);
        var localEtag = HexBytesUtil.bytes2hex(hashBytes)

        var etag : String = localEtag

        if (!local) {
          var etag = objstore.put(simulatedSignalContainer, outputFileName, dataBytes)
        }
        else { //if we are local, create a file output stream and include the private header information
          val FOS: FileOutputStream  = new FileOutputStream(new File(outputFileName));
          if (dataClass != "test" && dataClass != "basictest") {
            FOS.write(mapper.writeValueAsBytes(DS.privateHeader));
            FOS.write('\n');
          }
          FOS.write(dataBytes);  //this includes the public header already
          FOS.close();
        }

        if(etag != localEtag) {
          throw MisMatchDigest(s"$etag != $localEtag")
        }
        else {
          println("MD5 match okay")
        }

        //update database
        if (!local) {
          dashdb.etag(localEtag);
          dashdb.insertDataStatement.executeUpdate
        }

        //close noise generator
        noiseGen.close();
        

        //ship the file to object storage
        //if this fails, need to rollback as well!

      } catch {
          // ... code to handle exceptions
          // if DB connection exception, don't push data file to Object Storage. 
          //Need to print out info for logs. 
          case e : SQLException => {
            printException(e)

            try {
              println("SQLException: Transaction is being rolled back");
              objstore.delete(simulatedSignalContainer, outputFileName)
              dashdb.connection.rollback;
            } catch {
              case ee : Throwable => ee.printStackTrace
            }
          }
          case e : Throwable => {
            e.printStackTrace

            try {
              println("Other Throwable: Transaction is being rolled back");
              if(!local){
                dashdb.connection.rollback
                objstore.delete(simulatedSignalContainer, outputFileName)
              }
            } catch {
              case ee : Throwable => ee.printStackTrace
            }
          }
      }  
    }

    if (!local) {
      dashdb.insertDataStatement.close
      dashdb.connection.close
    }
  }
  
  def printException(ex: SQLException)  {
    if (ex != null) {
      System.err.println("SQLState: " + ex.getSQLState());
      System.err.println("Error Code: " + ex.getErrorCode());
      System.err.println("Message: " + ex.getMessage());
      var t:Throwable  = ex.getCause();
      while (t != null) {
        System.out.println("Cause: " + t);
        t = t.getCause();
      }      
    }
  }

  def main(args: Array[String]) {
    
    var dataClass = args(0)
    
    //really, I should move all these args to 'val's for this object,
    //then won't have to pass them in to the functions directly. 

    val simType:String = args(1)
    
    simType match {
      case "spark" => {
        val numPartitions:Int = args(2).toInt
        val nSims:Int = args(3).toInt
        val noiseName:String = args(5)
        sparkSim(numPartitions, nSims, args(4), noiseName, dataClass)
      }
      case "serial" => {
        val nSims:Int = args(2).toInt
        val noiseName:String = args(4)
        serialSim(nSims, args(3), noiseName, false, dataClass)
      }
      case "local" => {
        val nSims:Int = args(2).toInt
        val noiseName:String = args(4)
        serialSim(nSims, args(3), noiseName, true, dataClass)
      }
    }

  }
  
}
