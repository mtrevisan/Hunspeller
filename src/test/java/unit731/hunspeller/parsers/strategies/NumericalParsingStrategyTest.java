package unit731.hunspeller.parsers.strategies;

import unit731.hunspeller.parsers.affix.strategies.NumericalParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;


public class NumericalParsingStrategyTest{

	private final FlagParsingStrategy strategy = new NumericalParsingStrategy();


	@Test
	public void ok(){
		String[] flags = strategy.parseFlags("1,2");

		Assert.assertEquals(Arrays.asList("1", "2"), Arrays.asList(flags));
	}

	@Test(expected = IllegalArgumentException.class)
	public void notOk1(){
		strategy.parseFlags("ab");
	}

	@Test(expected = IllegalArgumentException.class)
	public void notOk2(){
		strategy.parseFlags("1.2");
	}

	@Test
	public void empty(){
		String[] flags = strategy.parseFlags("");

		Assert.assertEquals(0, flags.length);
	}

	@Test
	public void nullFlags(){
		String[] flags = strategy.parseFlags(null);

		Assert.assertEquals(0, flags.length);
	}

	@Test
	public void joinFlags(){
		String[] flags = new String[]{"1", "2"};
		String continuationFlags = strategy.joinFlags(flags);

		Assert.assertEquals("/1,2", continuationFlags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithError1(){
		String[] flags = new String[]{"1", "c"};
		strategy.joinFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithError2(){
		String[] flags = new String[]{"1", "1.2"};
		strategy.joinFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithEmpty(){
		String[] flags = new String[]{"1", ""};
		strategy.joinFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithNull(){
		String[] flags = new String[]{"ab", null};
		strategy.joinFlags(flags);
	}

	@Test
	public void joinEmptyFlags(){
		String[] flags = new String[]{};
		String continuationFlags = strategy.joinFlags(flags);

		Assert.assertTrue(continuationFlags.isEmpty());
	}

	@Test
	public void joinNullFlags(){
		String continuationFlags = strategy.joinFlags(null);

		Assert.assertTrue(continuationFlags.isEmpty());
	}
	
}
