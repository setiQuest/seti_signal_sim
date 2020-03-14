package org.seti.simulator.database

import java.sql._

import com.ibm.db2.jcc.DB2Driver

class DashDB(jdbc_url: String,
             user: String,
             pass: String,
             databasename: String) {

  val jdbcClassName = "com.ibm.db2.jcc.DB2Driver"
  Class.forName(jdbcClassName)
  java.sql.DriverManager.registerDriver(new DB2Driver)
  val connection: Connection = DriverManager.getConnection(jdbc_url, user, pass)

  var insertString = s"insert into $databasename values("
  val tableSize = 27
  //there are 27 parameters to add to the database
  //here's the format of that database

  // create table setiusers.simsignals (
  // uuid VARCHAR(128) not null,
  // sigma_noise DECIMAL(31,10),
  // noise_name  VARCHAR(128),
  // delta_phi_rad DECIMAL(31,10),
  // signal_to_noise_ratio DECIMAL(31,10),
  // drift DECIMAL(31,10),
  // drift_rate_derivative DECIMAL(31,10),
  // jitter DECIMAL(31,10),
  // len BIGINT,
  // amp_modulation_type VARCHAR(128),
  // amp_modulation_period DECIMAL(31,10),
  // amp_modulation_duty DECIMAL(31,10),
  // amp_phase DECIMAL(31,10),
  // amp_phase_square DECIMAL(31,10),
  // amp_phase_sine DECIMAL(31,10),
  // signal_classification VARCHAR(128),
  // seed BIGINT,
  // drift_divisor DECIMAL(31,10),
  // initial_sine_drift DECIMAL(31,10),
  // initial_cosine_drift DECIMAL(31,10),
  // simulator_software_version INT,
  // simulator_software_version_date VARCHAR(128),
  // date_created TIMESTAMP(10),
  // container VARCHAR(128),
  // objectname VARCHAR(128),
  // etag VARCHAR(256),
  // noise_file_uuid VARCHAR(128),
  // signal_power DECIMAL(31,10),
  // noise_power DECIMAL(31, 10),
  // );

  for (_ <- 1 until tableSize) {
    insertString += "?,"
  }
  insertString += "?)"

  val insertDataStatement: PreparedStatement =
    connection.prepareStatement(insertString)

  def uuid(uuid: String) {
    insertDataStatement.setString(1, uuid)
  }

  def sigN(sigN: Double) {
    insertDataStatement.setDouble(2, sigN)
  }

  def noiseName(noiseName: String) {
    insertDataStatement.setString(3, noiseName)
  }

  def dPhi(dPhi: Double) {
    insertDataStatement.setDouble(4, dPhi)
  }

  def SNR(SNR: Double) {
    insertDataStatement.setDouble(5, SNR)
  }

  def drift(drift: Double) {
    insertDataStatement.setDouble(6, drift)
  }

  def driftRateDerivative(driftRateDerivative: Double) {
    insertDataStatement.setDouble(7, driftRateDerivative)
  }

  def jitter(jitter: Double) {
    insertDataStatement.setDouble(8, jitter)
  }

  def len(len: Long) {
    insertDataStatement.setLong(9, len)
  }

  def ampModType(ampModType: String) {
    insertDataStatement.setString(10, ampModType)
  }

  def ampModPeriod(ampModPeriod: Double) {
    insertDataStatement.setDouble(11, ampModPeriod)
  }

  def ampModDuty(ampModDuty: Double) {
    insertDataStatement.setDouble(12, ampModDuty)
  }

  def ampPhase(ampPhase: Double) {
    insertDataStatement.setDouble(13, ampPhase)
  }

  def ampPhaseSquare(ampPhaseSquare: Double) {
    insertDataStatement.setDouble(14, ampPhaseSquare)
  }

  def ampPhaseSine(ampPhaseSine: Double) {
    insertDataStatement.setDouble(15, ampPhaseSine)
  }

  def signalClass(signalClass: String) {
    insertDataStatement.setString(16, signalClass)
  }

  def seed(seed: Long) {
    insertDataStatement.setLong(17, seed)
  }

  def mDriftDivisor(mDriftDivisor: Double) {
    insertDataStatement.setDouble(18, mDriftDivisor)
  }

  def sinDrift(sinDrift: Double) {
    insertDataStatement.setDouble(19, sinDrift)
  }

  def cosDrift(cosDrift: Double) {
    insertDataStatement.setDouble(20, cosDrift)
  }

  def simulationVersion(simulationVersion: Int) {
    insertDataStatement.setInt(21, simulationVersion)
  }

  def simulationVersionDate(simulationVersionDate: String) {
    insertDataStatement.setString(22, simulationVersionDate)
  }

  def time(time: Timestamp) {
    insertDataStatement.setTimestamp(23, time)
  }

  def container(container: String) {
    insertDataStatement.setString(24, container)
  }

  def outputFileName(outputFileName: String) {
    insertDataStatement.setString(25, outputFileName)
  }

  def etag(etag: String) {
    insertDataStatement.setString(26, etag)
  }

  def noise_file_uuid(noise_file_uuid: String) {
    insertDataStatement.setString(27, noise_file_uuid)
  }

  def signalPower(signalPower: Double) {
    insertDataStatement.setDouble(28, signalPower)
  }

  def noisePower(noisePower: Double) {
    insertDataStatement.setDouble(29, noisePower)
  }

  //needed data retrieval queries
  def get_sun_noise(num: Int): ResultSet = {
    val statement = connection.createStatement()
    val getStatement =
      "select uuid, container, objectname from setiusers.sunnoise where used = ? limit ?"
    val getPreparedStatement: PreparedStatement =
      connection.prepareStatement(getStatement)
    getPreparedStatement.setString(1, "False")
    getPreparedStatement.setInt(2, num)

    getPreparedStatement.executeQuery
  }

  //update sun noise table
  def update_sun_noise_usage(uuid: String, inUse: String): Int = {
    val statement = connection.createStatement()

    val updateStatement =
      "update setiusers.sunnoise set used = ? where uuid = ?"
    val updatePreparedStatement: PreparedStatement =
      connection.prepareStatement(updateStatement)
    updatePreparedStatement.setString(1, inUse)
    updatePreparedStatement.setString(2, uuid)

    updatePreparedStatement.executeUpdate
  }

  // def update_sun_noise_usages(uuids: Array[String], inUse: Boolean) : Int = {
  //   val statement = connection.createStatement()

  //   val updateStatement = "update setiusers.sunnoise set used = ? where uuid in (?)"
  //   val updatePreparedStatement: PreparedStatement = connection.prepareStatement(updateStatement)
  //   updatePreparedStatement.setBoolean(1, inUse)

  //   updatePreparedStatement.setArray(2, uuids)

  //   updatePreparedStatement.executeUpdate
  // }

}
