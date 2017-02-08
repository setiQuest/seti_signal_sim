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

import scala.collection.JavaConverters._

import com.fasterxml.jackson.databind.ObjectMapper;

import org.seti.simulator.errors.MisMatchDigest
import org.seti.simulator.utils.HexBytesUtil
import org.seti.simulator.objectstorage.ObjectStore
import org.seti.simulator.database.DashDB
import org.seti.simulator.parameters.ParameterGenerator
import org.seti.simulator.parameters.ParameterSet

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


  def makeNoiseGen(noiseName: String, seed: Long) : NoiseGenerator =  {
    if (noiseName == "gaussian") {
      return new GaussianNoise(seed);
    }
    else {
      return new FileNoise(noiseName);
    }
  }

  def runSim(paramGen: ParameterGenerator) {
    
  }

  def sparkSim(numPartitions: Int, nSim: Int, paramGenName: String, noiseName: String) {
    //paramGenName is the same as the signal class!

    val conf = new SparkConf().setAppName("SETI Sim")
    val sc = new SparkContext(conf)

    val initSeed: Long = System.currentTimeMillis()*nSim
    var container = "simsignals"

    // var credentials = scala.collection.mutable.HashMap[String, String](
    //   "auth_url"->"https://identity.open.softlayer.com",
    //   "project"->sys.env("SWIFT_TENANT"),
    //   "project_id"->sys.env("SWIFT_TENANT_ID"),
    //   "region"->"dallas",
    //   "user_id"->sys.env("SWIFT_API_USER"),
    //   "domain_id"->sys.env("SWIFT_API_DOMAIN_ID"),
    //   "domain_name"->sys.env("SWIFT_API_DOMAIN"),
    //   "password"->sys.env("SWIFT_API_KEY")
    // )
    var credentials = scala.collection.mutable.HashMap[String, String](
      "auth_url"->"https://identity.open.softlayer.com",
      "project"->"object_storage_8d3d095b_43e0_449a_ab52_49f26a243623",
      "project_id"->"cdbef52bdf7a449c96936e1071f0a46b",
      "region"->"dallas",
      "user_id"->"5c5f55667fb846f29946c9f1f0e0f3db",
      "domain_id"->"11b0d7dcb99e42c7a5e742a6aa7977af",
      "domain_name"->"798995",
      "password"->"v!2Q#!!]ODW7cx,T"
    )
    var configurationName : String = "setipublic"
    //val bmos = new bluemix(sc, configurationName, credentials)
    
    
    var jdbcurl:String = "jdbc:db2://dashdb-enterprise-yp-dal09-47.services.dal.bluemix.net:50001/BLUDB:sslConnection=true;"
    var dashuser : String = "adamcox"
    var dashpass : String = "Lepton12bluDashDB"

    var rdd = sc.parallelize(0 to nSim, numPartitions)  

    var rdd2 = rdd.map(i => {
        
      var mConf: Configuration = new Configuration(true)
      mConf.set("fs.swift2d.impl","com.ibm.stocator.fs.ObjectStoreFileSystem");
      mConf.set(s"fs.swift2d.service.$configurationName.auth.url", "https://identity.open.softlayer.com/v3/auth/tokens");
      mConf.set(s"fs.swift2d.service.$configurationName.public", "true");
      mConf.set(s"fs.swift2d.service.$configurationName.tenant", "cdbef52bdf7a449c96936e1071f0a46b");
      mConf.set(s"fs.swift2d.service.$configurationName.password", "v!2Q#!!]ODW7cx,T");
      mConf.set(s"fs.swift2d.service.$configurationName.username", "5c5f55667fb846f29946c9f1f0e0f3db");
      mConf.set(s"fs.swift2d.service.$configurationName.region", "dallas");

      var seed:Long = initSeed + i.toLong
     
      var uuid:String = UUID.randomUUID().toString()

      var paramGen = new ParameterGenerator(paramGenName)
      var params:ParameterSet = paramGen.next
      val noiseGen = makeNoiseGen(noiseName, seed)
      noiseGen.setAmp(params.sigmaN);

      var DS = new DataSimulator(noiseGen, params.sigmaN, params.deltaPhiRad, params.SNR, params.drift, 
        params.driftRateDerivate, params.sigmaSquiggle, params.outputLength, params.ampModType, params.ampModPeriod, 
        params.ampModDuty, params.signalClass, seed, uuid);

      var dataOutputByteStream = new ByteArrayOutputStream(params.outputLength);

      //only add the public header to output byte stream.
      var mapper = new ObjectMapper();
      var json = mapper.writeValueAsString(DS.publicHeader);
      System.out.println(json);
      dataOutputByteStream.write(mapper.writeValueAsBytes(DS.publicHeader));
      dataOutputByteStream.write('\n');

      DS.run(dataOutputByteStream)

      //close noise generator
      // THIS will need to change with the FileNoise generator. 
      // Will need something like a singleton class that can deliver noise files
      // Or, perhaps from the Sun data, can create a large number of random noise files from that
      // and distribute them on Spark (hmm... could use that distribution of noise files
      // as the start of building an RDD by doing a sc.binaryFiles which will return an RDD of noise bytes)
      noiseGen.close();

      val digest:MessageDigest = MessageDigest.getInstance("MD5");
      var hashBytes = digest.digest(dataOutputByteStream.toByteArray);
      var localEtag = HexBytesUtil.bytes2hex(hashBytes)
    
      //val dashdbSlow : DashDB = new DashDB(sys.env("JDBC_URL"), sys.env("DASHDBUSER"), sys.env("DASHDBPASS"))  //will need to use a connection pool. 
      
      val dashdbSlow : DashDB = new DashDB(jdbcurl, dashuser, dashpass)  //will need to use a connection pool. 
      var status = "success"
      var p:String  = s"swift2d://$container.$configurationName/$uuid.dat";
      var fs:FileSystem = null

      try {
         
        dashdbSlow.uuid(DS.uuid)
        dashdbSlow.sigN(DS.sigN);
        dashdbSlow.noiseName(noiseName);
        dashdbSlow.dPhi(DS.dPhi);
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
        dashdbSlow.container(container);
        dashdbSlow.outputFileName(s"$uuid.dat");
        dashdbSlow.etag(localEtag);
        
        fs = FileSystem.get(URI.create(p), mConf);
        var out: FSDataOutputStream =  fs.create(new Path(p));
        out.write("abcdefgh".getBytes());
        out.close();
        //etag = objstore.put(ofn, new ByteArrayInputStream(dataBytes))

        // var fschecksum = fs.getFileChecksum(new Path(p))
        // fschecksum is null... 
        // var etag = fschecksum.toString
        
        // if(etag != localEtag {
        //   throw MisMatchDigest(s"$etag != $localEtag")
        // }
        // else {
        //   println("MD5 match okay")
        //   println("checksum algorith: " + fschecksum.getAlgorithmName)
        // }

        //update database
        dashdbSlow.insertDataStatement.executeUpdate

      } catch {
          // ... code to handle exceptions
          // if DB connection exception, don't push data file to Object Storage. 
          //Need to print out info for logs. 
          case e : SQLException => {
            printException(e)
            status = "failed"
            try {
              println("Transaction is being rolled back");
              dashdbSlow.connection.rollback;
              fs.delete(new Path(p), false);
            } catch {
              case ee : Throwable => ee.printStackTrace
            }
          }
          case e : Throwable => {
            e.printStackTrace
            status = "failed"
            try {
              println("Transaction is being rolled back");
              dashdbSlow.connection.rollback
              fs.delete(new Path(p), false);
            } catch {
              case ee : Throwable => ee.printStackTrace
            }
          }
      }  finally {
        //return database connections to the pool!
        dashdbSlow.insertDataStatement.close
        dashdbSlow.connection.close

      }

      (seed, uuid, status)
    })  

    //rdd2.count()
    var rdd3 = rdd2.filter(i => {i._3 == "success"})

    println("Completed " + rdd3.count() + " simulations out of " + nSim + " requested of type " +  paramGenName)
    
    //println("Generated " + rdd6.count() + " simulations")
    sc.stop()
  }

  def serialSim(nSim: Int, paramGenName: String, noiseName: String) {

    val objstore : ObjectStore = new ObjectStore(sys.env("SWIFT_API_USER"), sys.env("SWIFT_API_KEY"), sys.env("SWIFT_AUTH_URL"), sys.env("SWIFT_API_DOMAIN"), sys.env("SWIFT_TENANT"))
    val dashdb: DashDB = new DashDB(sys.env("JDBC_URL"), sys.env("DASHDBUSER"), sys.env("DASHDBPASS"))  
    val paramGen:ParameterGenerator = new ParameterGenerator(paramGenName)

    for (i <- 1 to nSim) {
      val seed: Long = System.currentTimeMillis()
      val noiseGen = makeNoiseGen(noiseName, seed)
      var params: ParameterSet = paramGen.next
      noiseGen.setAmp(params.sigmaN);
      var uuid:String = UUID.randomUUID().toString()

      //  output file name is based on uuid
      var outputFileName = uuid + ".dat"

      println(params.toString)
      println("file: " + outputFileName + "\n")

      var DS = new DataSimulator(noiseGen, params.sigmaN, params.deltaPhiRad, params.SNR, params.drift, 
        params.driftRateDerivate, params.sigmaSquiggle, params.outputLength, params.ampModType, params.ampModPeriod, 
        params.ampModDuty, params.signalClass, seed, uuid);
    
      var dataOutputByteStream = new ByteArrayOutputStream(params.outputLength);

      //only add the public header to output byte stream.

      var mapper = new ObjectMapper();
      var json = mapper.writeValueAsString(DS.publicHeader);
      System.out.println(json);
      dataOutputByteStream.write(mapper.writeValueAsBytes(DS.publicHeader));
      dataOutputByteStream.write('\n');

      try {
            

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
        dashdb.container(objstore.container);
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

        var etag = objstore.put(outputFileName, new ByteArrayInputStream(dataBytes))

        //make sure
        dashdb.etag(etag);

        //calculate local md5 and compare to returned etag
        val digest:MessageDigest = MessageDigest.getInstance("MD5");
        var hashBytes = digest.digest(dataBytes);
        var localEtag = HexBytesUtil.bytes2hex(hashBytes)

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
              objstore.delete(outputFileName)
            } catch {
              case ee : Throwable => ee.printStackTrace
            }
          }
          case e : Throwable => {
            e.printStackTrace

            try {
              println("Transaction is being rolled back");
              dashdb.connection.rollback
              objstore.delete(outputFileName)
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
    
    val simType:String = args(0)
    
    if (simType == "spark") {
      val numPartitions:Int = args(1).toInt
      val nSims:Int = args(2).toInt
      val noiseName:String = args(4)
      sparkSim(numPartitions, nSims, args(3), noiseName)
    }
    else {
      val nSims:Int = args(1).toInt
      val noiseName:String = args(3)
      serialSim(nSims, args(2), noiseName)
    }

  }
  
}