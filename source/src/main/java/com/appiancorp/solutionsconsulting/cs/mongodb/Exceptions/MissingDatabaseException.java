package com.appiancorp.solutionsconsulting.cs.mongodb.Exceptions;

public class MissingDatabaseException extends Exception {
    public MissingDatabaseException(String databaseName) {
        super("Database '" + databaseName + "' does not exist.");
    }
}
