package org.kaldax.cli.commands;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;

import org.reflections.Reflections;

public class CommandFactory {

	public static ArrayList<AbstractCommand> getCommands(String[] args) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		ArrayList<AbstractCommand> result = new ArrayList<AbstractCommand>();
		for (int i = 0; i < args.length;) {
			String command = args[i];
			Reflections reflections = new Reflections("de.kwsoft.qa.casanova");
			Set<Class<? extends AbstractCommand>> allClasses = reflections.getSubTypesOf(AbstractCommand.class);
			boolean commandFound = false;
			for (Class<? extends AbstractCommand> c : allClasses) {
				Command[] ec2CommandAnno = c.getAnnotationsByType(Command.class);
				if (ec2CommandAnno != null) {
					String cmdLine = ec2CommandAnno[0].cmdLine();
					if (cmdLine.equals(command)) {
						System.out.println("Command: " + command);
						Constructor<?> ctor = c.getConstructor();
						AbstractCommand commandObject = (AbstractCommand) ctor.newInstance(new AbstractCommand [] {});
						i = commandObject.configure(args, i);
						result.add(commandObject);
						if(args.length==i) {
							return result;
						}
						commandFound=true;
						break;
					}
				}
			}
			if(!commandFound) {
				throw new IllegalArgumentException("Unknown command: " + command);
			}
		}
		return result;
	}

}
