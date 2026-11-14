
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import java.net.ServerSocket

ServerSocket server = context.get("smtpServer")
List messages = context.get("smtpMessages")
server.close()

// the SMTP transport sends one message per recipient
assert messages.size() == 3
String mail = messages.join( "\n" )
assert mail.contains( "MAIL FROM: <second@example.org>" )
assert mail.contains( "RCPT TO: <to1@example.org>" )
assert mail.contains( "RCPT TO: <to2@example.org>" )
assert mail.contains( "RCPT TO: <cc@example.org>" )
assert mail.contains( "Subject: [ANNOUNCEMENT] - Mail Project 1.1 released" )
assert mail.contains( "Second Developer" )
// the forked announcement-generate goal produced the body
assert mail.contains( "Changes in this version include:" )
assert mail.contains( "o Added additional documentation on how to configure the plugin." )
assert new File( basedir, "target/announcement/announcement.vm" ).exists()
