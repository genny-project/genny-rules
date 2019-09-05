package life.genny.test;

import static org.junit.Assert.*;

import org.junit.Test;

import life.genny.models.GennyToken;

public class saafal {

	public boolean isDuplicate(char x, String newString ) {
		for(char x1 : newString.toCharArray()) {
			if( Character.toUpperCase(x1) == Character.toUpperCase(x)) {
				return true;
			}
		}
		return false;
	}
	
	@Test
	public void beFetchTest() {
		String a = "abcdefghijklmn123123opqrstuvw"
				+ "xyzssssssssssssddasdasdkjashdkjasccewyuuwoiqweqwuibdcwiudh9832102983mxiuawhmi"
				+ "ouh9003019092;;'.dksuinwijdijeijidjscsdcdscsdsiddijsdijdijisdfi393020dybvmlpw"
				+ "62tipcmmy name is safjsldk saochuicsdhcsicusc hudiasousewenvvn vodhfdcujdcndcju"
				+ "kshfnds sdlfjsdlkfdsfjeieohooijh23o23n jiosjdofijsdiofjsd iwfsdjfiojfiffjfhasds"
				+ "hjbcbuedqwiquhwdbsabdxcdiushfasdkashdwudbsikbskajdaksjdhwubcdsdsadssdsdsdsdsds";
		
		int duplicate= 0;
		String newString = "";
		
		for(char x : a.toCharArray()) {			
			if(isDuplicate(x, newString)) {
				
				duplicate++;
				continue;
			}
			newString+=x;
		}
		
		
		System.out.println("There are total " + duplicate + " number of duplicate" +" in String [" + a +"]");
		System.out.println("new String = " + newString);

	}

}
