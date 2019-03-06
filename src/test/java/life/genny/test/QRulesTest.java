package life.genny.test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.vavr.Tuple2;
import life.genny.qwanda.Answer;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeBoolean;
import life.genny.qwanda.attribute.AttributeDate;
import life.genny.qwanda.attribute.AttributeDateTime;
import life.genny.qwanda.attribute.AttributeDouble;
import life.genny.qwanda.attribute.AttributeLong;
import life.genny.qwanda.attribute.AttributeText;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.Person;
import life.genny.qwanda.exception.BadDataException;
import life.genny.rules.QRules;

public class QRulesTest {
	@Test
	public void scoringTest()
	{
		System.out.println("This is a scoring TEst");
		
		// Create a set of baseentities to test
		AttributeText favouriteColour = new AttributeText(AttributeText.getDefaultCodePrefix()+"COLOUR","Favourite Colour");
		AttributeText favouriteBand = new AttributeText(AttributeText.getDefaultCodePrefix()+"BAND","Fasvourite Band");
		AttributeText favouriteAnimal = new AttributeText(AttributeText.getDefaultCodePrefix()+"ANIMAL","Favourite Animal");
		
		Attribute attributeDouble = new AttributeDouble(AttributeDouble.getDefaultCodePrefix()+"TEST4","Test Double 4");
		Attribute attributeLong = new AttributeLong(AttributeLong.getDefaultCodePrefix()+"TEST5","Test Long 5");	
		Attribute attributeBoolean = new AttributeBoolean(AttributeBoolean.getDefaultCodePrefix()+"TEST6","Test Boolean 6");	
		Attribute attributeDateTime = new AttributeDateTime(AttributeDateTime.getDefaultCodePrefix()+"TEST7","Test DateTiume 7");	
		Attribute attributeDate = new AttributeDate(AttributeDate.getDefaultCodePrefix()+"TEST8","Test Date 8");	
		
		
		BaseEntity source = new Person("PER_LONELY_GUY","Lonely Guy");
		BaseEntity target1 = new Person("PER_CONTESTANT_1","Contestant 1");
		BaseEntity target2 = new Person("PER_CONTESTANT_2","Contestant 2");
		BaseEntity target3 = new Person("PER_CONTESTANT_3","Contestant 3");
		
		try {
			source.addAttribute(favouriteColour, 1.0,"RED");
			source.addAttribute(favouriteBand, 0.8,"Fleetwood Mac");
			source.addAttribute(favouriteAnimal, 0.6, "DOG");
//			source.addAttribute(attributeDouble, 0.6, 3.141);
//			source.addAttribute(attributeLong, 0.6, 3147L);
//			source.addAttribute(attributeBoolean, 0.6, true);
//			source.addAttribute(attributeDateTime, 0.6, LocalDateTime.of(2017, Month.JUNE, 20, 10, 13));
//			source.addAttribute(attributeDate, 0.6, LocalDate.of(2017, Month.JUNE, 20));

			target1.addAttribute(favouriteColour, 1.0,"RED");
			target1.addAttribute(favouriteBand, 0.8,"AC/DC");
			target1.addAttribute(favouriteAnimal, 0.6, "CAT");
			
			target2.addAttribute(favouriteColour, 1.0,"BLUE");
			target2.addAttribute(favouriteBand, 0.8,"Fleetwood Mac");
			target2.addAttribute(favouriteAnimal, 0.6, "HORSE");

			target3.addAttribute(favouriteColour, 1.0,"RED");
			target3.addAttribute(favouriteBand, 0.8,"Nickleback");
			target3.addAttribute(favouriteAnimal, 0.6, "DOG");

			List<BaseEntity> targets = new ArrayList<BaseEntity>();
			targets.add(target1);
			targets.add(target2);
			targets.add(target3);
			
			
			List<Tuple2<BaseEntity,Double>> scores = QRules.score(source,targets,2); /* find the top 3 sorted scores against the source */
	 		
	 		/* Now save the top scores as attributes in the source */
	 		
	 		List<Answer> answerScores = new ArrayList<Answer>();
	 		Integer scoreIndex = 1;
	 		for (Tuple2<BaseEntity,Double> score : scores ) {
				Answer answer = new Answer(source.getCode(), source.getCode(), "PRI_SCORE_"+scoreIndex, score._1.getCode());
	 			answer.setWeight(score._2);
				answerScores.add(answer);
				scoreIndex++;
				
				System.out.println(answer);
			}
			
	 		
			
		} catch (BadDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
}
