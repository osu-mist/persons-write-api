package edu.oregonstate.mist.personswrite

import edu.oregonstate.mist.api.Application
import edu.oregonstate.mist.api.Configuration
import io.dropwizard.jdbi.DBIFactory
import io.dropwizard.setup.Environment
import org.skife.jdbi.v2.DBI

/**
 * Main application class.
 */
class PersonsWriteApplication extends Application<PersonsWriteConfiguration> {
    /**
     * Parses command-line arguments and runs the application.
     *
     * @param configuration
     * @param environment
     */
    @Override
    public void run(PersonsWriteApplication configuration, Environment environment) {
        this.setup(configuration, environment)

        DBIFactory factory = new DBIFactory()
        DBI jdbi = factory.build(environment, configuration.getDataSourceFactory(), "jdbi")
    }

    /**
     * Instantiates the application class with command-line arguments.
     *
     * @param arguments
     * @throws Exception
     */
    public static void main(String[] arguments) throws Exception {
        new PersonsWriteApplication().run(arguments)
    }
}
