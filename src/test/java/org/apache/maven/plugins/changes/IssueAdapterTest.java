package org.apache.maven.plugins.changes;

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

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import org.apache.maven.plugins.changes.model.Action;
import org.apache.maven.plugins.changes.model.Release;
import org.apache.maven.plugins.issues.Issue;
import org.apache.maven.plugins.issues.IssueManagementSystem;
import org.apache.maven.plugins.jira.JIRAIssueManagmentSystem;

/**
 * @author Alan Parkinson
 * @version $Id$
 * @since 2.6
 */
public class IssueAdapterTest extends TestCase
{

    public void testDefaultIssueTypeMapping()
    {
        IssueAdapter adapter = new IssueAdapter( new JIRAIssueManagmentSystem() );

        Issue issue = createIssue( "TST-1", "New Feature" );
        Action action = adapter.createAction( issue );
        assertEquals( "add", action.getType() );

        issue = createIssue( "TST-2", "Bug" );
        action = adapter.createAction( issue );
        assertEquals( "fix", action.getType() );

        issue = createIssue( "TST-3", "Improvement" );
        action = adapter.createAction( issue );
        assertEquals( "update", action.getType() );

        issue = createIssue( "TST-4", "Unknown Type" );
        action = adapter.createAction( issue );
        assertEquals( "", action.getType() );
    }

    public void testCustomIssueTypeMappingOveridesDefaultMapping()
    {
        IssueManagementSystem ims = new JIRAIssueManagmentSystem();

        ims.getIssueTypeMap().clear();
        IssueAdapter adapter = new IssueAdapter( ims );

        Issue issue = createIssue( "TST-1", "New Feature" );
        Action action = adapter.createAction( issue );
        assertEquals( "", action.getType() );

        issue = createIssue( "TST-2", "Bug" );
        action = adapter.createAction( issue );
        assertEquals( "", action.getType() );

        issue = createIssue( "TST-3", "Improvement" );
        action = adapter.createAction( issue );
        assertEquals( "", action.getType() );

        issue = createIssue( "TST-4", "Unknown Type" );
        action = adapter.createAction( issue );
        assertEquals( "", action.getType() );
    }

    public void testCustomIssueTypeMapping()
    {
        IssueManagementSystem ims = new JIRAIssueManagmentSystem();
        ims.getIssueTypeMap().put( "Story", IssueType.ADD );
        ims.getIssueTypeMap().put( "Epic", IssueType.ADD );
        ims.getIssueTypeMap().put( "Defect", IssueType.FIX );
        ims.getIssueTypeMap().put( "Error", IssueType.FIX );
        IssueAdapter adapter = new IssueAdapter( ims );

        Issue issue = createIssue( "TST-1", "Story" );
        Action action = adapter.createAction( issue );
        assertEquals( "add", action.getType() );

        issue = createIssue( "TST-2", "Epic" );
        action = adapter.createAction( issue );
        assertEquals( "add", action.getType() );

        issue = createIssue( "TST-3", "Error" );
        action = adapter.createAction( issue );
        assertEquals( "fix", action.getType() );

        issue = createIssue( "TST-4", "Defect" );
        action = adapter.createAction( issue );
        assertEquals( "fix", action.getType() );

        // Test the default mapping for "update" hasn't been overridden
        issue = createIssue( "TST-5", "Improvement" );
        action = adapter.createAction( issue );
        assertEquals( "update", action.getType() );
    }

    private Issue createIssue( String key, String type )
    {
        return createIssue( key, type, null );
    }

    private Issue createIssue( String key, String type, String version )
    {
        Issue issue = new Issue();
        issue.setKey( key );
        issue.setType( type );
        if ( version != null )
        {
            issue.addFixVersion( version );
        }

        issue.setAssignee( "A User" );
        issue.setSummary( "The title of this issue" );
        return issue;
    }

    public void testReleaseOrder()
    {
        IssueManagementSystem ims = new JIRAIssueManagmentSystem();
        ims.getIssueTypeMap().put( "Story", IssueType.ADD );
        ims.getIssueTypeMap().put( "Epic", IssueType.ADD );
        ims.getIssueTypeMap().put( "Defect", IssueType.FIX );
        ims.getIssueTypeMap().put( "Error", IssueType.FIX );
        IssueAdapter adapter = new IssueAdapter( ims );

        List<Issue> issues =
                Arrays.asList( createIssue( "TST-1", "Story", "1.0.0-alpha" ), createIssue( "TST-2", "Epic", "1.2.1" ),
                        createIssue( "TST-3", "Error", "0.1.1" ), createIssue( "TST-4", "Defect", "3.0" ),
                        createIssue( "TST-5", "Improvement", "4" ), createIssue( "TST-6", "Epic", "0.1.1" ) );

        List<Release> releases = adapter.getReleases( issues );

        assertEquals( releases.size(), 5 );
        assertEquals( releases.get( 0 ).getVersion(), "4" );
        assertEquals( releases.get( 1 ).getVersion(), "3.0" );
        assertEquals( releases.get( 2 ).getVersion(), "1.2.1" );
        assertEquals( releases.get( 3 ).getVersion(), "1.0.0-alpha" );
        assertEquals( releases.get( 4 ).getVersion(), "0.1.1" );
    }
}
