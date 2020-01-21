package unit731.hunlinter.parsers.workers.exceptions;


public class HunLintException extends RuntimeException{

	public enum FixActionType{ADD, REPLACE, REMOVE}


	//FIXME useful?
	private final Runnable fix;
	private final FixActionType fixActionType;


	public HunLintException(final String description){
		this(description, null, null);
	}

	public HunLintException(final String description, final Runnable fix, final FixActionType fixActionType){
		super(description);

		this.fix = fix;
		this.fixActionType = fixActionType;
	}

	public boolean canFix(){
		return (fix != null);
	}

	public Runnable getFixAction(){
		return fix;
	}

	public FixActionType getFixActionType(){
		return fixActionType;
	}

}