package com.thomsonreuters.exception;

public class InvalidTokenException extends Exception {
	private static final long serialVersionUID = 2854499606014010864L;

	public InvalidTokenException( String msg ) {
		super( msg );
	}
	
	public InvalidTokenException( Throwable e ) {
		super( e );
	}
}
