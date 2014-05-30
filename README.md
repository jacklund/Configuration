# Configuration
The purpose of this project is to create a Java configuration framework that's simple and lightweight, and that allows you to configure your system either from the command line, or from a configuration file, or both.

## Design
One of the tradeoffs you have to make in designing a system is what to make configurable, and what not to. If you don't make enough of the system configurable, you end up having to edit code and rebuild it when you want to make a simple change. If you make too much of it configurable, then you end up with a plethora of configuration files, and a potential configuration headache.

The criterion should be that you should only configure those items that are likely to change with regular use. Put another way, something which significantly alters the way your system works (e.g, swapping out implementations for a given interface) probably should *not* be part of your configuration, unless you're realistically planning on changing it from day to day.

What this project is attempting to do is to construct a configuration framework which will allow you to make your configuration calls at the top level (where it belongs), but have it be able to configure objects at any level of the object hierarchy without any of the other objects having to know anything about configuration. The configuration framework traverses the object hierarchy looking for your client object; once it finds it, it calls setters on the object corresponding to the configuration information. Thus, your objects wouldn't have to know anything about configuration, and they wouldn't have to pass configuration values down to where they're needed.

Naturally, this isn't going to work in all cases, especially as the object hierarchies get larger and more complex - this isn't a one-size-fits-all solution. However, for reasonably small services which require configuration, this should help keep them lightweight, configuration-wise.

## Dependencies
To do the command-line parsing, I'm using [args4j](http://args4j.kohsuke.org/), which is a nifty Java command-line configuration framework. For configuration files, the current plan is to support JSON files via [Jackson](http://jackson.codehaus.org/).

## Usage
Typically, you would decorate the classes that require configuration with the appropriate annotations from args4j and Jackson (if necessary):

```java
public class Foo {
  private String bar;
  private int baz;
  
  public String getBar() {
    return bar;
  }
  
  @Option(name = "--bar", usage = "the bar value")
  public void setBar(String b) {
    bar = b;
  }
  
  public int getBaz() {
    return baz;
  }
  
  @Option(name = "--baz", usage = "the baz value")
  public void setBaz(int b) {
    baz = b;
  }
}

public class FooBar {
  private Foo foo = new Foo();
  
  public Foo getFoo() {
    return foo;
  }
}

public class MyService {
  public static void main(String[] args) {
    FooBar foobar = new FooBar();
    CommandLineParser parser = new CommandLineParser(foobar);
    parser.parse(args);
  }
}
```

In this case, if you passed "``--bar myBarValue --baz 23``" on the command line, it would set the ``Foo`` instance in ``foobar`` with a ``bar`` value of "myBarValue", and a ``baz`` value of 23. Similarly, you could do the same thing in a configuration file with the following code:

```java
public class MyService {
  public static void main(String[] args) {
    FooBar foobar = new FooBar();
    JsonConfigFileParser parser = new JsonConfigFileParser("/path/to/config/file");
    parser.configure(foobar);
  }
}
```

and JSON file:

```json
{
  "foo": {
    "bar": "myBarValue",
    "baz": 23
  }
}
```
