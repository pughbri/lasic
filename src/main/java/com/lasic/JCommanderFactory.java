package com.lasic;

import com.beust.jcommander.JCommander;

/**
 * Factory to disambiguate which constructor to call for Scala.
 * For details on why this is needed see http://stackoverflow.com/questions/3313929/how-do-i-disambiguate-in-scala-between-methods-with-vararg-and-without
 * @author Brian Pugh
 */
public class JCommanderFactory {
  public static JCommander createWithArgs(Object cmdLineArgs) {
    return new JCommander(cmdLineArgs);
  }
}
