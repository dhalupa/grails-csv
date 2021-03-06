package org.grails.plugins.csv

import au.com.bytecode.opencsv.CSVReader
import grails.plugins.*
import org.grails.plugins.csv.CSVMapReader
import org.grails.plugins.csv.CSVReaderUtils
import org.grails.plugins.csv.controller.RenderCsvMethod

class CsvGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.0.1 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def author = "Les Hazlewood"
    def authorEmail = "les@katasoft.com"
    def title = "Grails CSV Plugin"
    def description = '''
        The Grails CSV Plugin allows you to easily parse and consume CSV data from a number of input sources.  It
        supports complex parsing scenarios such as nested commas inside quotes, escaped tokens, multi-line quoted
        values and allows configuration of parsing options (separator char, escape char, text encoding, etc).  It is
        based on Glen Smith (et. al.)'s OpenCSV project (http://opencsv.sourceforge.net)

        This plugin adds two dynamic methods 'eachCsvLine' and 'toCsvReader' to each of the following classes:
        - java.lang.String
        - java.io.File
        - java.io.InputStream
        - java.io.Reader

        Using it is extremely simple.  On any instance of the four data types, call the 'eachCvsLine' method with a
          closure accepting the tokens (a String array) for each parsed line:

        "hello, world, how, are, you".eachCsvLine { tokens ->
            //only one line in this case and tokens.length == 5
        }

        new File("iso3166Countries.csv").eachCsvLine { tokens ->
            new Country(tokens[0],        //ISO 3166 country name
                        tokens[1]).save() //ISO 3166 2 letter character code
        }

        Configuration

        If you need to specify how the parsing should occur, you can construct your own csv reader with a map of
        configuration options and call the 'eachLine' method on the constructed reader:

        anInputStream.toCsvReader(['charset':'UTF-8']).eachLine { tokens ->
            ...
        }

        The supported config options:

        'separatorChar': the character to use as the delimiter to separate the tokens.  Defaults to the comma: ','

        'quoteChar': the character indicating a quoted string is about to follow.  Internal separatorChars can be
                     inside the quoted string and they will not be split into tokens.
                     Defaults to the double quote char: '"'

        'escapeChar': the character to escape an immediately following character, indicating to the parser not to treat
                      it as a special char.  Defaults to the backslash char: '\'

        'skipLines': the number of lines in the input source to skip before parsing begins.  This is useful to skip
                     any potential CSV header lines that are not part of the CSV data.  Defaults to zero '0'

        'strictQuotes': if characters outside of quotes should be ignored (implying each individual token is
                        quoted.  Defaults to false

        'ignoreLeadingWhiteSpace': white space in front of a quoted token is ignored.  Defaults to true

        'charset': use the specified charset when parsing an InputStream.  The value can be either the Charset name
                   as a String, a java.nio.charset.Charset instance, or a java.nio.charset.CharsetDecoder instance.
                   Defaults to the system default charset.
                   *Note that this option is ONLY valid for InputStream instances.  It is ignored otherwise.

    '''
    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/csv"

    def profiles = ['web']

    def renderCsvMethod = { Map args, Closure definition ->
        new RenderCsvMethod(delegate).call(args, definition)
        false
    }

    Closure doWithSpring() { {->
            // TODO Implement runtime spring config (optional)
        } 
    }

    void doWithDynamicMethods() {
        //TODO this should really all be in a utility method so it can easily be called for unit testing
        CSVReader.metaClass.eachLine = { closure ->
            CSVReaderUtils.eachLine((CSVReader) delegate, closure)
        }

        File.metaClass.eachCsvLine = { closure ->
            CSVReaderUtils.eachLine((File) delegate, closure)
        }
        File.metaClass.toCsvReader = { settingsMap ->
            return CSVReaderUtils.toCsvReader((File)delegate, settingsMap)
        }
        File.metaClass.toCsvMapReader = { settingsMap ->
            return new CSVMapReader(new FileReader(delegate),settingsMap)
        }

        InputStream.metaClass.eachCsvLine = { closure ->
            CSVReaderUtils.eachLine((InputStream) delegate, closure)
        }
        InputStream.metaClass.toCsvReader = { settingsMap ->
            return CSVReaderUtils.toCsvReader((InputStream)delegate, settingsMap)
        }

        Reader.metaClass.eachCsvLine = { closure ->
            CSVReaderUtils.eachLine((Reader)delegate, closure)
        }
        Reader.metaClass.toCsvReader = { settingsMap ->
            return CSVReaderUtils.toCsvReader((Reader)delegate, settingsMap)
        }
        Reader.metaClass.toCsvMapReader = { settingsMap ->
            return new CSVMapReader(delegate,settingsMap)
        }

        String.metaClass.eachCsvLine = { closure ->
            CSVReaderUtils.eachLine((String) delegate, closure)
        }
        String.metaClass.toCsvReader = { settingsMap ->
            return CSVReaderUtils.toCsvReader((String)delegate, settingsMap)
        }
        String.metaClass.toCsvMapReader = { settingsMap ->
            return new CSVMapReader(new StringReader(delegate),settingsMap)
        }

        grailsApplication.controllerClasses.each {
            it.clazz.metaClass.renderCsv = renderCsvMethod
        }
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        if (grailsApplication.isControllerClass(event.source)) {
            event.source.metaClass.renderCsv = renderCsvMethod
        }
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
