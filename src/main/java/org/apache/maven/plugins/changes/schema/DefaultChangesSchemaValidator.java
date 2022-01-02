package org.apache.maven.plugins.changes.schema;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.io.input.XmlStreamReader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.IOUtil;
import org.xml.sax.SAXException;

/**
 * @author Olivier Lamy
 * @since 28 juil. 2008
 * @version $Id$
 */
@Component( role = ChangesSchemaValidator.class, hint = "default" )
public class DefaultChangesSchemaValidator
    implements ChangesSchemaValidator
{

    /** property schema */
    public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    public static final String CHANGES_SCHEMA_PATH = "META-INF/changes/xsd/";

    private Map<String, Schema> compiledSchemas = new HashMap<>();

    public XmlValidationHandler validateXmlWithSchema( File file, String schemaVersion, boolean failOnValidationError )
        throws SchemaValidatorException
    {
        try
        {
            String schemaPath = CHANGES_SCHEMA_PATH + "changes-" + schemaVersion + ".xsd";

            Schema schema = getSchema( schemaPath );

            Validator validator = schema.newValidator();

            XmlValidationHandler baseHandler = new XmlValidationHandler( failOnValidationError );

            validator.setErrorHandler( baseHandler );

            try ( Reader reader = new XmlStreamReader( file ) )
            {
                validator.validate( new StreamSource( reader ) );
            }

            return baseHandler;
        }
        catch ( IOException e )
        {
            throw new SchemaValidatorException( "IOException : " + e.getMessage(), e );
        }
        catch ( SAXException e )
        {
            throw new SchemaValidatorException( "SAXException : " + e.getMessage(), e );
        }
        catch ( Exception e )
        {
            throw new SchemaValidatorException( "Exception : " + e.getMessage(), e );
        }
    }

    public Schema getSchema( String schemaPath )
        throws SAXException, IOException
    {
        if ( this.compiledSchemas.containsKey( schemaPath ) )
        {
            return this.compiledSchemas.get( schemaPath );
        }
        Schema schema = this.compileJAXPSchema( schemaPath );

        this.compiledSchemas.put( schemaPath, schema );

        return schema;
    }

    /**
     * @param uriSchema
     * @return Schema
     * @throws Exception
     */
    private Schema compileJAXPSchema( String uriSchema )
        throws IOException, SAXException, NullPointerException
    {
        InputStream in = null;
        try
        {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream( uriSchema );
            if ( in == null )
            {
                throw new NullPointerException( " impossible to load schema with path " + uriSchema );
            }

            //newInstance de SchemaFactory not ThreadSafe
            final Schema schema = SchemaFactory.newInstance( W3C_XML_SCHEMA ).newSchema( new StreamSource( in ) );
            in.close();
            in = null;
            return schema;
        }
        finally
        {
            IOUtil.close( in );
        }
    }

    public void loadSchema( String uriSchema )
        throws SchemaValidatorException
    {
        try
        {
            this.getSchema( uriSchema );
        }
        catch ( SAXException e )
        {
            throw new SchemaValidatorException( "SAXException : " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new SchemaValidatorException( "IOException : " + e.getMessage(), e );
        }

    }
}
