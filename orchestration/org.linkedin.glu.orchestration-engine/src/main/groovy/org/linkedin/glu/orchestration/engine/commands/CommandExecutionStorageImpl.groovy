/*
 * Copyright (c) 2012 Yan Pujante
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.linkedin.glu.orchestration.engine.commands

import org.linkedin.glu.orchestration.engine.commands.CommandExecution.CommandType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author yan@pongasoft.com */
public class CommandExecutionStorageImpl implements CommandExecutionStorage
{
  public static final String MODULE = CommandExecutionStorageImpl.class.getName ();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  private def _streams = [:]

  @Override
  CommandExecution startExecution(String fabric,
                                  String agent,
                                  String username,
                                  String command,
                                  String commandId,
                                  CommandType commandType,
                                  long startTime)
  {
    CommandExecution.withTransaction {
      CommandExecution res = new CommandExecution(fabric: fabric,
                                                  agent: agent,
                                                  username: username,
                                                  command: command,
                                                  commandId: commandId,
                                                  commandType: commandType,
                                                  startTime: startTime)

      if(!res.save())
        throw new Exception("cannot save command execution ${commandId}: ${res.errors}")
      
      return res
    }
  }

  @Override
  CommandExecution endExecution(String commandId,
                                long endTime,
                                String stdinFirstBtes,
                                String stdoutFirstBytes,
                                String stderrFirstBytes,
                                String exitValue)
  {
    CommandExecution.withTransaction {
      CommandExecution execution = CommandExecution.findByCommandId(commandId)
      if(!execution)
      {
        log.warn("could not find command execution ${commandId}")
      }
      else
      {
        execution.endTime = endTime
        execution.stdinFirstBytes = stdinFirstBtes
        execution.stdoutFirstBytes = stdoutFirstBytes
        execution.stderrFirstBytes = stderrFirstBytes
        execution.exitValue = exitValue

        if(!execution.save())
        {
          log.warn("could not save command execution ${commandId}")
        }
      }
      return execution
    }
  }

  @Override
  OutputStream getStdinOutputStream(String commandId)
  {
    synchronized(_streams)
    {
      def streams = _streams[commandId] ?: [:]
      streams.stdin = new ByteArrayOutputStream()
      _streams[commandId] = streams

      return streams.stdin
    }
  }

  @Override
  InputStream getStdinInputStream(String commandId)
  {
    synchronized(_streams)
    {
      ByteArrayOutputStream baos = _streams[commandId]?.stdin ?: new ByteArrayOutputStream()
      return new ByteArrayInputStream(baos.toByteArray())
    }
  }

  @Override
  OutputStream getResultOutputStream(String commandId)
  {
    synchronized(_streams)
    {
      def streams = _streams[commandId] ?: [:]
      streams.result = new ByteArrayOutputStream()
      _streams[commandId] = streams

      return streams.result
    }
  }

  @Override
  InputStream getResultInputStream(String commandId)
  {
    synchronized(_streams)
    {
      ByteArrayOutputStream baos = _streams[commandId]?.result ?: new ByteArrayOutputStream()
      return new ByteArrayInputStream(baos.toByteArray())
    }
  }


}