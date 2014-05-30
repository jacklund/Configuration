package net.geekheads.configuration;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;

import net.geekheads.configuration.CommandLineParser;
import net.geekheads.configuration.ConfigurationException;

import org.junit.Test;
import org.kohsuke.args4j.Option;

public class CommandLineParserTest {
	public static class TopLevel {
		private String foo;
		private NextLevel bar;
		
		public String getFoo() {
			return foo;
		}
		
		@Option(name="--foo", usage="foo", metaVar="f")
		public void setFoo(String foo) {
			this.foo = foo;
		}
		
		public NextLevel getBar() {
			return bar;
		}
		
		public void setBar(NextLevel bar) {
			this.bar = bar;
		}
	}
	
	public static class NextLevel {
		private int foobar;
		private String barfoo;
		
		public int getFoobar() {
			return foobar;
		}
		
		@Option(name = "--foobar", usage="foobar")
		public void setFoobar(int foobar) {
			this.foobar = foobar;
		}

		public String getBarfoo() {
			return barfoo;
		}

		@Option(name = "--barfoo", usage = "barfoo")
		public void setBarfoo(String barfoo) {
			this.barfoo = barfoo;
		}
	}

	@Test
	public void test() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ConfigurationException {
		TopLevel top = new TopLevel();
		top.setBar(new NextLevel());
		CommandLineParser parser = new CommandLineParser(top);
		parser.usage("Test", System.err);
		parser.parse("--foo", "foo", "--foobar", "23", "--barfoo", "barfoo");
		assertEquals("foo", top.getFoo());
		assertEquals(23, top.getBar().getFoobar());
		assertEquals("barfoo", top.getBar().getBarfoo());
	}

}
