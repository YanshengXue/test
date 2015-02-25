package com.thomsonreuters.exception;

public class ResourceTemporaryUnavailableException extends Exception {

	private static final long serialVersionUID = 7482348514333657344L;

	public ResourceTemporaryUnavailableException() {
	}

	public ResourceTemporaryUnavailableException(String arg0) {
		super(arg0);
	}

	public ResourceTemporaryUnavailableException(Throwable arg0) {
		super(arg0);
	}

	public ResourceTemporaryUnavailableException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public ResourceTemporaryUnavailableException(String arg0, Throwable arg1,
			boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
