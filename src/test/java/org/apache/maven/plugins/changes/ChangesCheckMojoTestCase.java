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
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static org.hamcrest.CoreMatchers.startsWith;

import org.apache.maven.plugins.changes.ChangesCheckMojo;
import org.junit.Test;

/**
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public class ChangesCheckMojoTestCase
{
    @Test
    public void testIsValidDate()
    {
        String pattern;

        // null pattern
        pattern = null;
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "2010-12-16", pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern ) );

        // empty pattern
        pattern = "";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "2010-12-16", pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern ) );

        // valid pattern
        pattern = "yyyy-MM-dd";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "2010-DD-MM", pattern ) );
        assertTrue( ChangesCheckMojo.isValidDate( "2010-12-16", pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern ) );
    }

    @Test
    public void testIsValidateWithLocale()
    {
        String pattern, locale = null;

        // null locale
        pattern = "yyyy-MM-dd";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern, locale ) );
        assertTrue( ChangesCheckMojo.isValidDate( "2010-12-06", pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern, locale ) );

        // unknown locale specified, should use default locale
        locale = "ab_CD";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern, locale ) );
        assertTrue( ChangesCheckMojo.isValidDate( "2010-12-06", pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern, locale ) );

        // pattern with months as number
        pattern = "yyyy-MM-dd";

        // Czech locale
        locale = "cs_CZ";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern, locale ) );
        assertTrue( ChangesCheckMojo.isValidDate( "2010-12-06", pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern, locale ) );

        // English locale
        locale = "en_US";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern, locale ) );
        assertTrue( ChangesCheckMojo.isValidDate( "2010-12-06", pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern, locale ) );

        // pattern with months as text
        pattern = "dd MMM yyyy";

        // English locale
        locale = "en_US";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern, locale ) );
        assertTrue( ChangesCheckMojo.isValidDate( "06 Dec 2010", pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern, locale ) );
    }
    
    // In JDK 9, the Unicode Consortium's Common Locale Data Repository (CLDR) data is enabled as the default locale data, 
    //   so that you can use standard locale data without any further action.
    // In JDK 8, although CLDR locale data is bundled with the JRE, it isn’t enabled by default.
    // source: https://docs.oracle.com/javase/9/migrate/toc.htm#JSMIG-GUID-A20F2989-BFA9-482D-8618-6CBB4BAAE310
    @Test
    public void testCompat()
    {
        // @TODO fix for Java 9+
        // System.setProperty( "java.locale.providers", "COMPAT,CLDR" ) is not picked up...
        assumeThat( System.getProperty( "java.version" ), startsWith( "1." ) );
        
        // pattern with months as text
        String pattern = "dd MMM yyyy";

        String originalJavaLocaleProviders = null;
        if ( !System.getProperty( "java.version" ).startsWith( "1." ) )
        {
            originalJavaLocaleProviders = System.setProperty( "java.locale.providers", "COMPAT,CLDR" );
        }
        try
        {
            // Czech locale
            String locale = "cs_CZ";
            assertFalse( ChangesCheckMojo.isValidDate( null, pattern, locale ) );
            assertFalse( ChangesCheckMojo.isValidDate( "", pattern, locale ) );
            assertTrue( ChangesCheckMojo.isValidDate( "06 XII 2010", pattern, locale ) );
            assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern, locale ) );
        }
        finally
        {
            if ( originalJavaLocaleProviders != null )
            {
                System.setProperty( "java.locale.providers", originalJavaLocaleProviders );
            }
            else
            {
                System.clearProperty( "java.locale.providers" );
            }
        }
    }
    
}
