package life.genny.rules;

public class JBPM {

	  private static final JBPM INSTANCE = new JBPM();

	  public static JBPM getInstance() {

	    return INSTANCE;

	  }

	  public void sayHello(String name) {

	    System.out.println("Hello " + name);

	  }

	}