package net.geekheads.configuration;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;

import net.geekheads.configuration.ConfigurationException;
import net.geekheads.configuration.JsonConfigFileParser;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonRootName;
import org.junit.Test;

public class JsonConfigFileParserTest {
	@JsonRootName(value = "top")
	public static class TopLevel {
		private String foo;
		private NextLevel bar;
		
		public String getFoo() {
			return foo;
		}
		
		public void setFoo(String foo) {
			this.foo = foo;
		}

		@JsonProperty(value = "next")
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
		
		public void setFoobar(int foobar) {
			this.foobar = foobar;
		}

		public String getBarfoo() {
			return barfoo;
		}

		@JsonProperty(value="barf")
		public void setBarfoo(String barfoo) {
			this.barfoo = barfoo;
		}
	}
	
	public static class Other {
		private String other;

		public String getOther() {
			return other;
		}

		public void setOther(String other) {
			this.other = other;
		}
	}

	@Test
	public void test() throws FileNotFoundException, ConfigurationException {
		TopLevel top = new TopLevel();
		top.setBar(new NextLevel());
		Other other = new Other();
		assertNotEquals("foo", top.getFoo());
		assertNotEquals(23, top.getBar().getFoobar());
		assertNotEquals("barfoo", top.getBar().getBarfoo());
		JsonConfigFileParser parser = new JsonConfigFileParser("src/test/resources/JsonConfigFileParserTest.json");
		parser.configure(top, other);
		assertEquals("foo", top.getFoo());
		assertNotNull(top.getBar());
		assertEquals(23, top.getBar().getFoobar());
		assertEquals("barfoo", top.getBar().getBarfoo());
		assertEquals("other", other.getOther());
	}

}
