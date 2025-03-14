// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2020-2022 MariaDB Corporation Ab

package org.mariadb.r2dbc.integration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.mariadb.r2dbc.BaseConnectionTest;
import org.mariadb.r2dbc.MariadbConnectionConfiguration;
import org.mariadb.r2dbc.MariadbConnectionFactory;
import org.mariadb.r2dbc.TestConfiguration;
import org.mariadb.r2dbc.api.MariadbConnection;
import org.mariadb.r2dbc.api.MariadbConnectionMetadata;
import org.slf4j.LoggerFactory;
import reactor.test.StepVerifier;

public class LoggingTest extends BaseConnectionTest {

  @Test
  void basicLogging() throws IOException {
    Assumptions.assumeFalse(isXpand());
    File tempFile = File.createTempFile("log", ".tmp");

    Logger logger = (Logger) LoggerFactory.getLogger("org.mariadb.r2dbc");
    Level initialLevel = logger.getLevel();
    logger.setLevel(Level.TRACE);
    logger.setAdditive(false);
    // logger.detachAndStopAllAppenders();

    LoggerContext context = new LoggerContext();
    FileAppender<ILoggingEvent> fa = new FileAppender<ILoggingEvent>();
    fa.setName("FILE");
    fa.setImmediateFlush(true);
    PatternLayoutEncoder pa = new PatternLayoutEncoder();
    pa.setPattern("%r %5p %c [%t] - %m%n");
    pa.setContext(context);
    pa.start();
    fa.setEncoder(pa);

    fa.setFile(tempFile.getPath());
    fa.setAppend(true);
    fa.setContext(context);
    fa.start();

    logger.addAppender(fa);

    MariadbConnectionConfiguration conf = TestConfiguration.defaultBuilder.build();
    MariadbConnection connection = new MariadbConnectionFactory(conf).create().block();
    connection
        .createStatement("SELECT 1")
        .execute()
        .flatMap(r -> r.map((row, metadata) -> row.get(0, Integer.class)))
        .as(StepVerifier::create)
        .expectNext(1)
        .verifyComplete();
    MariadbConnectionMetadata meta = connection.getMetadata();
    connection.close().block();

    try {
      String contents = new String(Files.readAllBytes(Paths.get(tempFile.getPath())));
      String selectIsolation =
          "         +-------------------------------------------------+\r\n"
              + "         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |\r\n"
              + "+--------+-------------------------------------------------+----------------+\r\n"
              + "|00000000| 80 00 00 00 03 53 45 54 20 61 75 74 6f 63 6f 6d |.....SET autocom|\r\n"
              + "|00000010| 6d 69 74 3d 31 2c 74 78 5f 69 73 6f 6c 61 74 69 |mit=1,tx_isolati|\r\n"
              + "|00000020| 6f 6e 3d 27 52 45 50 45 41 54 41 42 4c 45 2d 52 |on='REPEATABLE-R|\r\n"
              + "|00000030| 45 41 44 27 2c 73 65 73 73 69 6f 6e 5f 74 72 61 |EAD',session_tra|\r\n"
              + "|00000040| 63 6b 5f 73 63 68 65 6d 61 3d 31 2c 73 65 73 73 |ck_schema=1,sess|\r\n"
              + "|00000050| 69 6f 6e 5f 74 72 61 63 6b 5f 73 79 73 74 65 6d |ion_track_system|\r\n"
              + "|00000060| 5f 76 61 72 69 61 62 6c 65 73 3d 27 61 75 74 6f |_variables='auto|\r\n"
              + "|00000070| 63 6f 6d 6d 69 74 2c 74 78 5f 69 73 6f 6c 61 74 |commit,tx_isolat|\r\n"
              + "|00000080| 69 6f 6e 27                                     |ion'            |\r\n"
              + "+--------+-------------------------------------------------+----------------+";
      String mysqlIsolation =
          "         +-------------------------------------------------+\r\n"
              + "         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |\r\n"
              + "+--------+-------------------------------------------------+----------------+\r\n"
              + "|00000000| 92 00 00 00 03 53 45 54 20 61 75 74 6f 63 6f 6d |.....SET autocom|\r\n"
              + "|00000010| 6d 69 74 3d 31 2c 74 72 61 6e 73 61 63 74 69 6f |mit=1,transactio|\r\n"
              + "|00000020| 6e 5f 69 73 6f 6c 61 74 69 6f 6e 3d 27 52 45 50 |n_isolation='REP|\r\n"
              + "|00000030| 45 41 54 41 42 4c 45 2d 52 45 41 44 27 2c 73 65 |EATABLE-READ',se|\r\n"
              + "|00000040| 73 73 69 6f 6e 5f 74 72 61 63 6b 5f 73 63 68 65 |ssion_track_sche|\r\n"
              + "|00000050| 6d 61 3d 31 2c 73 65 73 73 69 6f 6e 5f 74 72 61 |ma=1,session_tra|\r\n"
              + "|00000060| 63 6b 5f 73 79 73 74 65 6d 5f 76 61 72 69 61 62 |ck_system_variab|\r\n"
              + "|00000070| 6c 65 73 3d 27 61 75 74 6f 63 6f 6d 6d 69 74 2c |les='autocommit,|\r\n"
              + "|00000080| 74 72 61 6e 73 61 63 74 69 6f 6e 5f 69 73 6f 6c |transaction_isol|\r\n"
              + "|00000090| 61 74 69 6f 6e 27                               |ation'          |\r\n"
              + "+--------+-------------------------------------------------+----------------+";

      if (meta.isMariaDBServer()
          || (meta.getMajorVersion() < 8 && !meta.minVersion(5, 7, 20))
          || (meta.getMajorVersion() >= 8 && !meta.minVersion(8, 0, 3))) {
        Assertions.assertTrue(
            contents.contains(selectIsolation)
                || contents.contains(selectIsolation.replace("\r\n", "\n")),
            contents);
      } else {
        Assertions.assertTrue(
            contents.contains(mysqlIsolation)
                || contents.contains(mysqlIsolation.replace("\r\n", "\n")),
            contents);
      }

      String selectOne =
          "         +-------------------------------------------------+\r\n"
              + "         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |\r\n"
              + "+--------+-------------------------------------------------+----------------+\r\n"
              + "|00000000| 09 00 00 00 03 53 45 4c 45 43 54 20 31          |.....SELECT 1   |\r\n"
              + "+--------+-------------------------------------------------+----------------+";
      Assertions.assertTrue(
          contents.contains(selectOne) || contents.contains(selectOne.replace("\r\n", "\n")));
      logger.setLevel(initialLevel);
      logger.detachAppender(fa);
      logger.setAdditive(true);
    } catch (Throwable e) {
      e.printStackTrace();
      Assertions.fail();
    }
  }

  public String encodeHexString(byte[] byteArray) {
    StringBuffer hexStringBuffer = new StringBuffer();
    for (int i = 0; i < byteArray.length; i++) {
      hexStringBuffer.append(byteToHex(byteArray[i]));
    }
    return hexStringBuffer.toString();
  }

  public String byteToHex(byte num) {
    char[] hexDigits = new char[2];
    hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
    hexDigits[1] = Character.forDigit((num & 0xF), 16);
    return new String(hexDigits);
  }
}
