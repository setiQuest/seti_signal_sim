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
  var dataClass : String = "" //this will either be 'training', 'test', or 'private'



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


  def sparkSim(numPartitions: Int, nSim: Int, paramGenName: String, noiseName: String) {
    //paramGenName is the same as the signal class!

    val conf = new SparkConf().setAppName("SETI Sim")
    val sc = new SparkContext(conf)

    // sc.hadoopConfiguration.set(s"fs.stocator.MaxPerRoute", "100");
    // sc.hadoopConfiguration.set(s"fs.stocator.MaxTotal", "1000");
    // sc.hadoopConfiguration.set(s"fs.stocator.ReqConnectTimeout", "20000");
    // sc.hadoopConfiguration.set(s"fs.stocator.SoTimeout", "20000");
    // sc.hadoopConfiguration.set(s"fs.stocator.ReqConnectionRequestTimeout", "20000");
    // sc.hadoopConfiguration.set(s"fs.stocator.ReqSocketTimeout", "20000");

    val initSeed: Long = System.currentTimeMillis()*nSim*numPartitions

    // var simulationProperties = scala.collection.mutable.HashMap[String, String](
    //   "auth_url"->"https://identity.open.softlayer.com",
    //   "project"->sys.env("SWIFT_TENANT"),
    //   "project_id"->sys.env("SWIFT_TENANT_ID"),
    //   "region"->"dallas",
    //   "user_id"->sys.env("SWIFT_API_USER"),
    //   "domain_id"->sys.env("SWIFT_API_DOMAIN_ID"),
    //   "domain_name"->sys.env("SWIFT_API_DOMAIN"),
    //   "password"->sys.env("SWIFT_API_KEY")
    // )

    
    //val simulationProperties = new Properties
    //propfile.load("/simulation.properties")
    //var cl:ClassLoader = getClass().getClassLoader()
    //propfile.load(cl.getResourceAsStream("config.properties"))
    //simulationProperties.load(getClass.getResourceAsStream("/simulation.properties"))
    //scala.io.Source.fromResource("config.properties")

    // val mapper = new ObjectMapper() with ScalaObjectMapper
    // mapper.registerModule(DefaultScalaModule)
    // val simulationProperties = mapper.readValue[Map[String,String]](propfile.toString.replaceAll("=",":"))


    // ObjectMapper mapper = new ObjectMapper(); 
    // File from = new File(getClass.getResourceAsStream("/simulation.properties")); 
    // TypeReference<HashMap<String,Object>> typeRef 
    //         = new TypeReference<HashMap<String,Object>>() {};

    // HashMap<String,Object> o = mapper.readValue(from, typeRef); 

    // var simulationProperties = scala.collection.mutable.HashMap[String, String](
    //   "auth_url"->"https://identity.open.softlayer.com/v3",
    //   "project"->"object_storage_8d3d095b_43e0_449a_ab52_49f26a243623",
    //   "project_id"->"cdbef52bdf7a449c96936e1071f0a46b",
    //   "region"->"dallas",
    //   "user_id"->"5c5f55667fb846f29946c9f1f0e0f3db",
    //   "domain_id"->"11b0d7dcb99e42c7a5e742a6aa7977af",
    //   "domain_name"->"798995",
    //   "password"->"v!2Q#!!]ODW7cx,T"
    // )
    //val bmos = new bluemix(sc, configurationName, simulationProperties)
    
    
    // var jdbcurl:String = "jdbc:db2://dashdb-enterprise-yp-dal09-47.services.dal.bluemix.net:50001/BLUDB:sslConnection=true;"
    // var dashuser : String = "adamcox"
    // var dashpass : String = "Lepton12bluDashDB"

    // var jdbcurl:String = props.getProperty("JDBC_URL")
    // var dashuser : String = props.getProperty("DASHDBUSER")
    // var dashpass : String = props.getProperty("DASHDBPASS")
    //needed for the SwiftObjStore
    val props = new Properties
    var simulatedSignalContainer : String = ""

    props.load(getClass.getResourceAsStream("/simulation.properties"))
    dataClass match {
      case "test" => {
        simulatedSignalContainer = props.getProperty("test_data_container")
      }
      case "training" => {
        simulatedSignalContainer = props.getProperty("training_data_container")
      }
      case "private" => {
        simulatedSignalContainer = props.getProperty("private_data_container")
      }
      case _ => {
        println("Incorrect data class ($dataClass). Choose either 'test' or 'training'.")
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
      val dashdbSlow : DashDB = new DashDB(props.getProperty("JDBC_URL"), props.getProperty("DASHDBUSER"), props.getProperty("DASHDBPASS"), props.getProperty("databasename"))  //will need to use a connection pool. 
      var sunnoise = dashdbSlow.get_sun_noise(nSim)
      var counter: Int = 0
      println("filling sun noise")
      while ( sunnoise.next ) {

        if (rand.nextDouble > 0.27) {  //keep this fraction secret!  some number of the simulated noise files is gaussian!
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
        throw MissingSunNoise(s"Only found $counter noise files, expected $nSim")
      }

    }
    else {
      //build new rdd with (i, noiseName, "", "")
      for (i <- 0 until nSim) {
        noiseArray(i) = (i, noiseName, "", "")
      }
    }

    var rdd = sc.parallelize(noiseArray, numPartitions)
 

    println("Starting simulations... ")

    

    var rdd2 = rdd.mapPartitionsWithIndex { (indx, iter) =>
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

      mConf.set(s"fs.stocator.MaxPerRoute", "200");
      mConf.set(s"fs.stocator.MaxTotal", "1000");
      mConf.set(s"fs.stocator.SoTimeout", "10000");
      mConf.set(s"fs.stocator.ReqConnectTimeout", "10000");
      mConf.set(s"fs.stocator.ReqConnectionRequestTimeout", "10000");
      mConf.set(s"fs.stocator.ReqSocketTimeout", "10000");

      iter.map(i => {
          
        //
        // each row, i, is a tuple: (int, noisename, "", "") or (int, container, objectname, uuid)
        //


        val objstore: SwiftObjStore = new SwiftObjStore(mConf,configurationName)
        //val objstore : OpenStack4jObjectStore = new OpenStack4jObjectStore(props, configurationName)
        val dashdbSlow : DashDB = new DashDB(props.getProperty("JDBC_URL"), props.getProperty("DASHDBUSER"), props.getProperty("DASHDBPASS"), props.getProperty("databasename"))  //will need to use a connection pool. 

        var uuid:String = UUID.randomUUID().toString()
      
        var message = s"starting simulation $seed for $uuid\n"

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
            noiseGen = new SunNoise(i._2, i._3, objstore) 
            noiseGen.setAmp(1.0)
          }
        }

        //val noiseGen = makeNoiseGen(noiseName, seed, sigdef)

        var DS = new DataSimulator(noiseGen, sigdef.sigmaN, sigdef.deltaPhiRad, sigdef.SNR, sigdef.drift, 
          sigdef.driftRateDerivate, sigdef.sigmaSquiggle, sigdef.outputLength, sigdef.ampModType, sigdef.ampModPeriod, 
          sigdef.ampModDuty, sigdef.signalClass, seed, randGen, uuid);

        var dataOutputByteStream = new ByteArrayOutputStream(sigdef.outputLength);

        //only add the public header to output byte stream.
        var mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(DS.publicHeader);
        System.out.println(json);
        dataOutputByteStream.write(mapper.writeValueAsBytes(DS.publicHeader));
        dataOutputByteStream.write('\n');

        DS.run(dataOutputByteStream)

        noiseGen.close();

        val digest:MessageDigest = MessageDigest.getInstance("MD5");
        var hashBytes = digest.digest(dataOutputByteStream.toByteArray);
        var localEtag = HexBytesUtil.bytes2hex(hashBytes)
      
        //val dashdbSlow : DashDB = new DashDB(sys.env("JDBC_URL"), sys.env("DASHDBUSER"), sys.env("DASHDBPASS"))  //will need to use a connection pool. 
        
        var status = "success"

        var outputFileName = s"$uuid.dat"

        message += s"$outputFileName\n"
        message += "Starting database transfer\n"

        try {
          noiseGen match {
            case m:SunNoise => {
              dashdbSlow.update_sun_noise_usage(i._4, true)
              dashdbSlow.noise_file_uuid(i._4)
            }
            case _ => {
              dashdbSlow.noise_file_uuid("")
            }
          }

          
          dashdbSlow.uuid(DS.uuid)
          dashdbSlow.sigN(DS.sigN);
          dashdbSlow.noiseName(noiseGen.getName());
          dashdbSlow.dPhiRad(DS.dPhiRad);
          dashdbSlow.SNR(DS.SNR);
          dashdbSlow.drift(DS.drift);
          dashdbSlow.driftRateDerivative(DS.driftRateDerivate);
          dashdbSlow.jitter(DS.jitter);
          dashdbSlow.len(DS.len);
          dashdbSlow.ampModType(DS.ampModType);
          dashdbSlow.ampModPeriod(DS.ampModPeriod);
          dashdbSlow.ampModDuty(DS.ampModDuty);
          dashdbSlow.ampPhase(DS.ampPhase);
          dashdbSlow.ampPhaseSquare(DS.ampPhaseSquare);
          dashdbSlow.ampPhaseSine(DS.ampPhaseSine);
          dashdbSlow.signalClass(DS.signalClass);
          dashdbSlow.seed(DS.seed);
          dashdbSlow.mDriftDivisor(DS.mDriftDivisor);
          dashdbSlow.sinDrift(DS.sinDrift);
          dashdbSlow.cosDrift(DS.cosDrift);
          dashdbSlow.simulationVersion(DS.simulationVersion);
          dashdbSlow.simulationVersionDate(DS.simulationVersionDate);

          dashdbSlow.time(new Timestamp(System.currentTimeMillis()));
          dashdbSlow.container(simulatedSignalContainer);
          dashdbSlow.outputFileName(outputFileName);
          dashdbSlow.etag(localEtag);

          message += s"PUT to object store $simulatedSignalContainer, $outputFileName\n"


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

          dashdbSlow.insertDataStatement.executeUpdate

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
                dashdbSlow.connection.rollback;
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
                dashdbSlow.connection.rollback
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
          message += "Closing db prepared statement\n"
          dashdbSlow.insertDataStatement.close
          dashdbSlow.connection.close
        }
        println(s"$status")

        (seed, uuid, status, message, outputFileName)

      })  

      
    }

    //rdd2.count()
    //var rdd3 = rdd2.filter(i => {i._3 == "failed"})
    var results = rdd2.collect()

    //println("Completed " + rdd3.count() + " simulations out of " + nSim + " requested of type " +  paramGenName)
    //println("Failed " + rdd3.count() + " simulations out of " + nSim + " requested of type " +  paramGenName)
    

    println("Returned: " + results.length + " simulations out of " + nSim + " requested of type " +  paramGenName)
    //results.foreach(i => {println(i._4)})

    var success = results.filter(i => {i._3 == "success"})
    println("Successful: " + success.length + " simulations out of " + nSim + " requested of type " +  paramGenName)
    success.foreach(i => {println("generated file: " + i._5)})

    var failures = results.filter(i => {i._3 == "failed"})
    println("Failed messages")
    failures.foreach(i => {println(i._4)})

    //println("Generated " + rdd6.count() + " simulations")
    sc.stop()
  }

  def serialSim(nSim: Int, paramGenName: String, noiseName: String) {

    val props = new Properties
    var simulatedSignalContainer : String = ""
    
    props.load(getClass.getResourceAsStream("/simulation.properties"))
    dataClass match {
      case "test" => {
        simulatedSignalContainer = props.getProperty("test_data_container")
      }
      case "training" => {
        simulatedSignalContainer = props.getProperty("training_data_container")
      }
      case "private" => {
        simulatedSignalContainer = props.getProperty("private_data_container")
      }
      case _ => {
        println("Incorrect data class ($dataClass). Choose either 'test' or 'training'.")
        return
      }
    }

    val objstore : OpenStack4jObjectStore = new OpenStack4jObjectStore(props, configurationName)
    val dashdb: DashDB = new DashDB(props.getProperty("JDBC_URL"), props.getProperty("DASHDBUSER"), props.getProperty("DASHDBPASS"), props.getProperty("databasename"))  
    //val paramGen:ParameterGenerator = new ParameterGenerator(paramGenName)
    
    val seed: Long = System.currentTimeMillis()
    var randGen = new Random(seed)

    for (i <- 0 until nSim) {
      
      var sigdef = SignalDefFactory(paramGenName, randGen, dataClass)

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

      //  output file name is based on uuid
      var outputFileName = s"$uuid.dat"

      println(sigdef.toString)
      println("file: " + outputFileName + "\n")

      var DS = new DataSimulator(noiseGen, sigdef.sigmaN, sigdef.deltaPhiRad, sigdef.SNR, sigdef.drift, 
        sigdef.driftRateDerivate, sigdef.sigmaSquiggle, sigdef.outputLength, sigdef.ampModType, sigdef.ampModPeriod, 
        sigdef.ampModDuty, sigdef.signalClass, seed, randGen, uuid);
    
      var dataOutputByteStream = new ByteArrayOutputStream(sigdef.outputLength);

      //only add the public header to output byte stream.

      var mapper = new ObjectMapper();
      var json = mapper.writeValueAsString(DS.publicHeader);
      System.out.println(json);
      dataOutputByteStream.write(mapper.writeValueAsBytes(DS.publicHeader));
      dataOutputByteStream.write('\n');

      try {
            
        // noiseGen match {
        //   case m:SunNoise => {
        //     dashdb.update_sun_noise_usage(i._4, true)
        //     dashdb.noise_file_uuid(i._4)
        //   }
        //   case _ => {
        //     dashdb.noise_file_uuid("")
        //   }
        // }
        dashdb.noise_file_uuid("")
        
        dashdb.uuid(DS.uuid)
        dashdb.sigN(DS.sigN);
        dashdb.noiseName(noiseGen.getName());
        dashdb.dPhiRad(DS.dPhiRad);
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


        DS.run(dataOutputByteStream)

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
        //first, insert data file to object store
        //then verify it is there (should I do a md5 checksum?)

        //then, insert into to dashDB database
        //if that fails, need to remove data form object store as well as 
        //rollback dashdb transaction

        //upload output file
        var dataBytes = dataOutputByteStream.toByteArray();

        var etag = objstore.put(simulatedSignalContainer, outputFileName, dataBytes)


        //calculate local md5
        val digest:MessageDigest = MessageDigest.getInstance("MD5");
        var hashBytes = digest.digest(dataBytes);
        var localEtag = HexBytesUtil.bytes2hex(hashBytes)
        
        dashdb.etag(localEtag);

        if(etag != localEtag) {
          throw MisMatchDigest(s"$etag != $localEtag")
        }
        else {
          println("MD5 match okay")
        }

        //update database
        dashdb.insertDataStatement.executeUpdate
        
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
              println("Transaction is being rolled back");
              dashdb.connection.rollback;
              objstore.delete(simulatedSignalContainer, outputFileName)
            } catch {
              case ee : Throwable => ee.printStackTrace
            }
          }
          case e : Throwable => {
            e.printStackTrace

            try {
              println("Transaction is being rolled back");
              dashdb.connection.rollback
              objstore.delete(simulatedSignalContainer, outputFileName)
            } catch {
              case ee : Throwable => ee.printStackTrace
            }
          }
      }  
    }

    dashdb.insertDataStatement.close
    dashdb.connection.close

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
    
    dataClass = args(0)
    
    //really, I should move all these args to 'val's for this object,
    //then won't have to pass them in to the functions directly. 

    val simType:String = args(1)
    
    if (simType == "spark") {
      val numPartitions:Int = args(2).toInt
      val nSims:Int = args(3).toInt
      val noiseName:String = args(5)
      sparkSim(numPartitions, nSims, args(4), noiseName)
    }
    else {
      val nSims:Int = args(2).toInt
      val noiseName:String = args(4)
      serialSim(nSims, args(3), noiseName)
    }

  }
  
}