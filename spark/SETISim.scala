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

// import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
// import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
// import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.collection.JavaConverters._

import com.fasterxml.jackson.databind.ObjectMapper;

import org.seti.simulator.errors.MisMatchDigest
import org.seti.simulator.utils.HexBytesUtil
import org.seti.simulator.objectstorage.OpenStack4jObjectStore
import org.seti.simulator.objectstorage.SwiftObjStore
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
  var configurationName : String = "setipublic"
  var container : String = "simsignals"


  def makeNoiseGen(noiseName: String, seed: Long) : NoiseGenerator =  {
    if (noiseName == "gaussian") {
      return new GaussianNoise(seed);
    }
    else {
      return new FileNoise(noiseName);
    }
  }


  def sparkSim(numPartitions: Int, nSim: Int, paramGenName: String, noiseName: String) {
    //paramGenName is the same as the signal class!

    val conf = new SparkConf().setAppName("SETI Sim")
    val sc = new SparkContext(conf)

    val initSeed: Long = System.currentTimeMillis()*nSim

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

    val credentials = new Properties
    //propfile.load("simulation.properties")
    //var cl:ClassLoader = getClass().getClassLoader()
    //propfile.load(cl.getResourceAsStream("config.properties"))
    credentials.load(getClass.getResourceAsStream("/simulation.properties"))
    //scala.io.Source.fromResource("config.properties")

    // val mapper = new ObjectMapper() with ScalaObjectMapper
    // mapper.registerModule(DefaultScalaModule)
    // val credentials = mapper.readValue[Map[String,String]](propfile.toString.replaceAll("=",":"))


    // ObjectMapper mapper = new ObjectMapper(); 
    // File from = new File(getClass.getResourceAsStream("/simulation.properties")); 
    // TypeReference<HashMap<String,Object>> typeRef 
    //         = new TypeReference<HashMap<String,Object>>() {};

    // HashMap<String,Object> o = mapper.readValue(from, typeRef); 

    // var credentials = scala.collection.mutable.HashMap[String, String](
    //   "auth_url"->"https://identity.open.softlayer.com/v3",
    //   "project"->"object_storage_8d3d095b_43e0_449a_ab52_49f26a243623",
    //   "project_id"->"cdbef52bdf7a449c96936e1071f0a46b",
    //   "region"->"dallas",
    //   "user_id"->"5c5f55667fb846f29946c9f1f0e0f3db",
    //   "domain_id"->"11b0d7dcb99e42c7a5e742a6aa7977af",
    //   "domain_name"->"798995",
    //   "password"->"v!2Q#!!]ODW7cx,T"
    // )
    //val bmos = new bluemix(sc, configurationName, credentials)
    
    
    // var jdbcurl:String = "jdbc:db2://dashdb-enterprise-yp-dal09-47.services.dal.bluemix.net:50001/BLUDB:sslConnection=true;"
    // var dashuser : String = "adamcox"
    // var dashpass : String = "Lepton12bluDashDB"

    // var jdbcurl:String = credentials.getProperty("JDBC_URL")
    // var dashuser : String = credentials.getProperty("DASHDBUSER")
    // var dashpass : String = credentials.getProperty("DASHDBPASS")

    println(s"Found $credentials")

    println("Generating initial RDD")
    var rdd = sc.parallelize(1 to nSim, numPartitions)  

    println("Starting simulations... ")

    var rdd2 = rdd.map(i => {
        
      val objstore: SwiftObjStore = new SwiftObjStore(credentials, configurationName)
      //val objstore : OpenStack4jObjectStore = new OpenStack4jObjectStore(credentials, configurationName)

      var seed:Long = initSeed + i.toLong
     
      var uuid:String = UUID.randomUUID().toString()
    
      var message = s"starting simulation $seed for $uuid\n"


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
      
      val dashdbSlow : DashDB = new DashDB(credentials.getProperty("JDBC_URL"), credentials.getProperty("DASHDBUSER"), credentials.getProperty("DASHDBPASS"))  //will need to use a connection pool. 
      var status = "success"

      var outputFileName = s"$uuid.dat"

      message += s"$outputFileName\n"
      message += "Starting database transfer\n"

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
        dashdbSlow.outputFileName(outputFileName);
        dashdbSlow.etag(localEtag);
        
        message += s"PUT to object store $container, $outputFileName\n"


        objstore.put(container, outputFileName, dataOutputByteStream.toByteArray)
        
        
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
            message += s"${e.getMessage}\n"
            status = "failed"
            try {
              println("Transaction is being rolled back");
              dashdbSlow.connection.rollback;
              objstore.delete(container, outputFileName);  
            } catch {
              case ee : Throwable => ee.printStackTrace
            }
          }
          case e : Throwable => {
            e.printStackTrace
            status = "failed"
            message += s"${e.getMessage}\n"
            try {
              println("Transaction is being rolled back");
              dashdbSlow.connection.rollback
              objstore.delete(container, outputFileName);
            } catch {
              case ee : Throwable => ee.printStackTrace
            }
          }
      }  finally {
        //return database connections to the pool!
        dashdbSlow.insertDataStatement.close
        dashdbSlow.connection.close

      }
      println(s"$status")

      (seed, uuid, status, message)
    })  

    //rdd2.count()
    //var rdd3 = rdd2.filter(i => {i._3 == "failed"})
    var results = rdd2.collect()

    //println("Completed " + rdd3.count() + " simulations out of " + nSim + " requested of type " +  paramGenName)
    //println("Failed " + rdd3.count() + " simulations out of " + nSim + " requested of type " +  paramGenName)
    

    println("Returned: " + results.length + " simulations out of " + nSim + " requested of type " +  paramGenName)
    //results.foreach(i => {println(i._4)})

    var success = results.filter(i => {i._3 == "success"})
    println("Successful: " + success.length + " simulations out of " + nSim + " requested of type " +  paramGenName)

    var failures = results.filter(i => {i._3 == "failed"})
    println("Failed messages")
    failures.foreach(i => {println(i._4)})

    //println("Generated " + rdd6.count() + " simulations")
    sc.stop()
  }

  def serialSim(nSim: Int, paramGenName: String, noiseName: String) {

    val credentials = new Properties
    credentials.load(getClass.getResourceAsStream("/simulation.properties"))

    val objstore : OpenStack4jObjectStore = new OpenStack4jObjectStore(credentials, configurationName)
    val dashdb: DashDB = new DashDB(credentials.getProperty("JDBC_URL"), credentials.getProperty("DASHDBUSER"), credentials.getProperty("DASHDBPASS"))  
    val paramGen:ParameterGenerator = new ParameterGenerator(paramGenName)

    for (i <- 1 to nSim) {
      val seed: Long = System.currentTimeMillis()
      val noiseGen = makeNoiseGen(noiseName, seed)
      var params: ParameterSet = paramGen.next
      noiseGen.setAmp(params.sigmaN);
      var uuid:String = UUID.randomUUID().toString()

      //  output file name is based on uuid
      var outputFileName = s"$uuid.dat"

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
        dashdb.container(container);
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

        var etag = objstore.put(container, outputFileName, dataBytes)


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
              objstore.delete(container, outputFileName)
            } catch {
              case ee : Throwable => ee.printStackTrace
            }
          }
          case e : Throwable => {
            e.printStackTrace

            try {
              println("Transaction is being rolled back");
              dashdb.connection.rollback
              objstore.delete(container, outputFileName)
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