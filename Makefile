JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Attribute.java \
	Config.java \
	imp2.java 

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class 
