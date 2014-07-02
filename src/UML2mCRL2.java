import org.eclipse.emf.ecore.xmi.impl.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.*;
import org.eclipse.emf.ecore.resource.*;
import org.eclipse.emf.ecore.*;

import java.io.*;

import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import java.util.*;
import org.eclipse.emf.common.util.*;
import org.eclipse.uml2.uml.internal.impl.MessageOccurrenceSpecificationImpl;
import org.eclipse.uml2.uml.*;
import org.eclipse.uml2.uml.internal.impl.*;

class LoopProcess extends Process {
	String processSignature;
	String condition;
	String callSignature;

	public void addLoopSignature(String signature) {
		this.processSignature = signature;
	}

	public void addCondition(String condition) {
		this.condition = condition;
	}

	public void setCallSignature(String callSignature) {
		this.callSignature = callSignature;
	}

	public String getCallSignature() {
		return this.callSignature;
	}

	public String toString() {
		return super.toString() + "\n" + "condition:" + this.condition + "\n"
				+ "loop signature:" + this.processSignature + "\n";
	}

	public StringBuffer prepareForMCRL2() {

		StringBuffer buffer = new StringBuffer();
		buffer.append("proc " + this.processSignature + "= \n");
		buffer.append("\t(" + this.condition + ") -> ( \n");
		@SuppressWarnings("unchecked")
		LinkedList<String> copyInvocations = ((LinkedList<String>) invocations
				.clone());

		Iterator<String> it_invocations = invocations.iterator();
		int counter = 0;
		while (it_invocations.hasNext()) {
			counter++;
			buffer.append("\t");
			String step = (String) it_invocations.next();
			copyInvocations.remove();
			if (step.contains("synch_") || step.contains("asynch_")
					|| step.contains("loop") || step.endsWith("internal")) {
				buffer.append(step);
				if (counter == invocations.size()
						|| copyInvocations.peek().trim().startsWith(")"))
					buffer.append("\n");
				else
					buffer.append(". \n");
			} else {

				buffer.append(step + "\n");

			}
		}

		buffer.append("\t ." + this.getCallSignature() + ") \n");
		buffer.append("\t <> " + "\n \t\t internal");

		buffer.append(";\n\n");
		return buffer;

	}
}

class Process {
	String id = "1"; // fixed for now, needs to be inspected from Activity
	// diagrams
	org.eclipse.uml2.uml.Class classImpl;
	Operation operationImpl;
	String methodSignature;
	String methodReturnSignature;
	LinkedList<String> invocations = new LinkedList<String>();
	LinkedHashMap<String, String> opParametersIn = new LinkedHashMap<String, String>();
	LinkedHashMap<String, String> opParametersReturn = new LinkedHashMap<String, String>();
	LinkedHashMap<String, String> sumParameters = new LinkedHashMap<String, String>();
	Interaction enclosingInteraction;

	boolean isProcessed = false;
	int loopCounter = 0;

	public Process() {
	}

	public org.eclipse.uml2.uml.Class getClassImpl() {
		return this.classImpl;
	}

	public void setEnclosingInteraction(Interaction interaction) {
		this.enclosingInteraction = interaction;
	}

	public Interaction getEnclosingInteraction() {
		return this.enclosingInteraction;
	}

	public void addSumParameter(String key, String value) {
		sumParameters.put(key, value);
	}

	public LinkedHashMap<String, String> getSumParameters() {
		return this.sumParameters;
	}

	public Operation getOperationImpl() {
		return this.operationImpl;
	}

	public Collection<Parameter> getParameters(Operation operationarg) {
		return EcoreUtil.getObjectsByType(operationarg.eContents(),
				UMLPackage.Literals.PARAMETER);
	}

	public LinkedHashMap<String, String> getOpParametersIn() {
		fillOperationParameters();
		return this.opParametersIn;
	}

	public LinkedHashMap<String, String> getOpParametersReturn() {
		fillOperationParameters();
		return this.opParametersReturn;
	}

	private void fillOperationParameters() {

		Collection<Parameter> operationParameters = getParameters(this.operationImpl);
		Iterator<Parameter> parameters_iterator = operationParameters
				.iterator();

		while (parameters_iterator.hasNext()) {

			Parameter parameterFromCollection = (Parameter) parameters_iterator
					.next();

			// return (out) parameters
			if (parameterFromCollection.getDirection().toString()
					.equals("return")) {
				if (parameterFromCollection.getType().getClass()
						.equals(PrimitiveTypeImpl.class)) {
					this.opParametersReturn
							.put(parameterFromCollection.getName(),
									UML2mCRL2
											.determinePrimitiveType((PrimitiveTypeImpl) parameterFromCollection
													.getType()));
				}

				else {
					this.opParametersReturn.put(
							parameterFromCollection.getName(), "ClassObject");
				}

			}

			// input parameters
			else {

				if (parameterFromCollection.getType().getClass()
						.equals(PrimitiveTypeImpl.class)) {
					this.opParametersIn
							.put(parameterFromCollection.getName(),
									UML2mCRL2
											.determinePrimitiveType((PrimitiveTypeImpl) parameterFromCollection
													.getType()));
				}

				else {
					this.opParametersIn.put(parameterFromCollection.getName(),
							"ClassObject");
				}

			}

		}
	}

	public LinkedList<String> getInvocations() {
		return this.invocations;
	}

	public int getLoopCounter() {
		return this.loopCounter;
	}

	public Process(org.eclipse.uml2.uml.Class classImpl, Operation operationImpl) {
		this.classImpl = classImpl;
		this.operationImpl = operationImpl;
	}

	public void addMethodSignature(String signature) {
		this.methodSignature = signature;
	}

	public void addMethodReturnSignature(String returnSignature) {
		this.methodReturnSignature = returnSignature;
	}

	public void setClassImpl(org.eclipse.uml2.uml.Class classImpl) {
		this.classImpl = classImpl;
	}

	public void setOperationImpl(Operation operationImpl) {
		this.operationImpl = operationImpl;
	}

	public void addInvocation(String invocation) {
		invocations.add(invocation);
	}

	public void addAltFragment(String guard) {
		invocations.add("(" + guard + ")->(");
	}

	public void addOptFragment(String guard) {
		invocations.add("((" + guard + ")->(");
	}

	public void addBreakFragment(String guard) {
		invocations.add("(" + guard + ")->(");
	}

	public void closeOptFragment() {
		invocations.add(")<>internal)");
	}

	public void addEndAltFragment(boolean lastStep) {
		if (lastStep)
			invocations.add(")");
		else
			invocations.add(" ) <> ");
	}

	public void addCloseFragment(boolean withInternal) {
		if (withInternal)
			invocations.add("<> internal)");
		else
			invocations.add(")");
	}

	public void addCloseBreakFragment() {
		invocations.add(") <> ");
	}

	public void addBeginAltFragment() {
		invocations.add("(");
	}

	public void addCallLoopFragment(String operation) {
		loopCounter++;
		invocations.add(operation);

	}

	public boolean equals(Process anotherProc) {
		return (this.classImpl == anotherProc.classImpl && this.operationImpl == anotherProc.operationImpl);
	}

	public String toString() {
		if (classImpl == null && operationImpl == null)
			return "No class && operation signature" + "\n" // starter process
					+ invocations.toString() + "\n";
		else if (classImpl == null) // this case happens only in loop processes
			return "No classImpl ; YES operationImpl \n" + this.methodSignature
					+ "\n" + this.methodReturnSignature + "\n+" + " "
					+ invocations.toString() + "\n" + "operation:"
					+ this.operationImpl.getName() + "\n";
		else
			return "Everything in place \n" // regular process
					+ "Class:" + this.classImpl.getName()
					+ "\n"
					+ this.methodSignature + "\n"
					+ this.methodReturnSignature
					+ "\n+" + " " + invocations.toString()
					+ "\n"
					+ "operation:" + this.operationImpl.getName() + "\n";
	}

	public void setProcessed() {
		this.isProcessed = true;
	}

	public boolean isProcessed() {
		return this.isProcessed;
	}

	public boolean equalsClassNameANDOperationName(String className,
			String operationName) {
		return (this.classImpl.getName().equals(className) && this.operationImpl
				.getName().equals(operationName));
	}

	public StringBuffer prepareForMCRL2() {
		StringBuffer buffer = new StringBuffer();
		// this should imply a starter process, so there must
		// be a link with the activity diagrams
		if (classImpl == null && operationImpl == null) // FIXME for now we
														// consider a starter
														// process to have the
														// name of the enclosing
														// interaction
			buffer.append("proc " + this.getEnclosingInteraction().getName()
					+ "(id:Nat) = \n");
		else {
			buffer.append("proc " + this.classImpl.getName() + "_"
					+ this.operationImpl.getName() + "(id:Nat) = \n");
		}

		@SuppressWarnings("unchecked")
		LinkedList<String> copyInvocations = (LinkedList<String>) invocations
				.clone();
		Iterator<String> it_invocations = invocations.iterator();
		int counter = 0;
		while (it_invocations.hasNext()) {
			counter++;
			buffer.append("\t");
			String step = (String) it_invocations.next();
			copyInvocations.remove();
			if (step.contains("synch_") || step.contains("asynch_")
					|| step.contains("loop") || step.contains("internal")) {
				buffer.append(step);
				if (counter == invocations.size()
						|| copyInvocations.peek().trim().startsWith(")"))
					buffer.append("\n");
				else
					buffer.append(". \n");
			} else {

				buffer.append(step + "\n");

			}
		}

		//add recursion Proc P = ... .P;
		if (classImpl == null && operationImpl == null) // FIXME for now we
			// consider a starter
			// process to have the
			// name of the enclosing
			// interaction
			buffer.append(". " + this.getEnclosingInteraction().getName()
					+ "(id)");
		else {
			buffer.append(". " + this.classImpl.getName() + "_"
					+ this.operationImpl.getName() + "(id)");
		}
		//END_add recursion
		buffer.append(";\n\n");
		return buffer;
	}
}

public class UML2mCRL2 {

	public static LinkedList<Process> processes = new LinkedList<Process>();
	public static LinkedList<LoopProcess> loop_processes = new LinkedList<LoopProcess>();
	public static LinkedList<String> ClassType = new LinkedList<String>();
	public static LinkedList<String> ClassObject = new LinkedList<String>();
	public static LinkedList<String> OperationSignatures = new LinkedList<String>();
	public static HashMap<String, Stack<Process>> readyProcessesPerLifeline = new HashMap<String, Stack<Process>>();
	public static HashMap<String, Stack<Process>> busyProcessesPerLifeline = new HashMap<String, Stack<Process>>();
	public static LinkedList<String> SortString = new LinkedList<String>();
	public static HashMap<String, LinkedList<String>> starters = new HashMap<String, LinkedList<String>>();
	public static int loopCounter = 1;
	public static StringBuffer systemInit = new StringBuffer(
			"proc systemInit = ");
	public static FileWriter fstream;
	public static BufferedWriter outfile;

	public static void createSorts(org.eclipse.uml2.uml.Package rootPackage)
			throws IOException {
		fstream = new FileWriter("./model.mcrl2");
		outfile = new BufferedWriter(fstream);
		Collection<org.eclipse.uml2.uml.Class> classes = getClasses(rootPackage);
		Iterator<org.eclipse.uml2.uml.Class> classes_iterator = classes
				.iterator();

		// Loop UML classes
		while (classes_iterator.hasNext()) {
			org.eclipse.uml2.uml.Class classFromCollection = (org.eclipse.uml2.uml.Class) classes_iterator
					.next();
			if (!(classFromCollection instanceof Activity)) {
				ClassType.add(classFromCollection.getName());
				Collection<Operation> classOperations = getOperations(classFromCollection);
				Iterator<Operation> operations_iterator = classOperations
						.iterator();
				// loop operations of each UML class
				while (operations_iterator.hasNext()) {
					// each operation becomes a process
					Process newProcess = new Process();
					newProcess.setClassImpl(classFromCollection);
					Operation operationFromCollection = (Operation) operations_iterator
							.next();
					newProcess.setOperationImpl(operationFromCollection);
					StringBuffer operationSignature = new StringBuffer(
							operationFromCollection.getName());
					StringBuffer operationSignature_return = new StringBuffer(
							operationFromCollection.getName() + "_return");

					Collection<Parameter> operationParameters = getParameters(operationFromCollection);
					Iterator<Parameter> parameters_iterator = operationParameters
							.iterator();
					StringBuffer operationParametersIn = new StringBuffer();
					StringBuffer operationParametersReturn = new StringBuffer();
					// System.out.println("---PARAMETERS----");
					while (parameters_iterator.hasNext()) {

						Parameter parameterFromCollection = (Parameter) parameters_iterator
								.next();

						// return parameters
						if (parameterFromCollection.getDirection().toString()
								.equals("return")
								|| parameterFromCollection.getDirection()
										.toString().equals("out")) {
							operationParametersReturn
									.append(parameterFromCollection.getName()
											+ ":");
							if (parameterFromCollection.getType().getClass()
									.equals(PrimitiveTypeImpl.class))
								operationParametersReturn
										.append(determinePrimitiveType((PrimitiveTypeImpl) parameterFromCollection
												.getType()) + ",");
							else
								operationParametersReturn.append("ClassObject"
										+ ",");
						}

						// input parameters
						else {
							operationParametersIn
									.append(parameterFromCollection.getName()
											+ ":");
							if (parameterFromCollection.getType().getClass()
									.equals(PrimitiveTypeImpl.class))
								operationParametersIn
										.append(determinePrimitiveType((PrimitiveTypeImpl) parameterFromCollection
												.getType()) + ",");
							else
								operationParametersIn.append("ClassObject"
										+ ",");
						}

					}
					// System.out.println("---END_PARAMETERS----");
					if (operationParametersIn.length() != 0) {
						OperationSignatures.add(operationSignature.toString()
								+ "("
								+ operationParametersIn.toString().substring(0,
										operationParametersIn.length() - 1)
								+ ")");

						newProcess.addMethodSignature(operationSignature
								.toString()
								+ "("
								+ operationParametersIn.toString().substring(0,
										operationParametersIn.length() - 1)
								+ ")");
					} else {
						OperationSignatures.add(operationSignature.toString());
						newProcess.addMethodSignature(operationSignature
								.toString());
					}

					if (operationParametersReturn.length() != 0) {
						OperationSignatures.add(operationSignature_return
								.toString()
								+ "("
								+ operationParametersReturn.toString()
										.substring(
												0,
												operationParametersReturn
														.length() - 1) + ")");

						newProcess
								.addMethodReturnSignature(operationSignature_return
										.toString()
										+ "("
										+ operationParametersReturn.toString()
												.substring(
														0,
														operationParametersReturn
																.length() - 1)
										+ ")");

					} else {
						OperationSignatures.add(operationSignature_return
								.toString());
						newProcess
								.addMethodReturnSignature(operationSignature_return
										.toString());
					}
					processes.add(newProcess);

				}
				// System.out.println("--END_OPERATIONS-----");
			}

		}
		// System.out.println("--END_CLASSES-----");
		printSortsMCRL2();
	}

	public static void printSortsMCRL2() throws IOException {
		ListIterator<String> ClassType_st = ClassType.listIterator(0);
		outfile.write("%comment \n");
		outfile.write("% created:" + new Date() + "\n \n");
		outfile.write("%-------sorts--------- \n");
		// FIX: make the list of Method names unique.
		LinkedList<String>	areTheyUnique = new LinkedList<String>(new HashSet<String>(OperationSignatures));
		System.out.println("Are there duplicates? " + areTheyUnique.size() +  " vs " + OperationSignatures.size());
		
		if (ClassType.size() == 0) {
			System.err
					.println("Strange:there are no classes in the UML model.\nPlease check for possible problems.");
		} else {
			outfile.write("sort ClassType = struct \n");
			while (ClassType_st.hasNext()) {
				outfile.write("\t\t\t" + ClassType_st.next());
				if (ClassType_st.hasNext())
					outfile.write(" | \n");
				else
					outfile.write(" ; \n");
			}
		}

		if (OperationSignatures.size() == 0) {
			System.err
					.println("Strange:there are no class operations in the UML model.\nPlease check for possible problems.");
		} else {
			outfile.write("sort Method = struct ");
			outfile.newLine();
			// FIX: was 			ListIterator<String> OperationSignatures_st = OperationSignatures.listIterator(0);
			ListIterator<String> OperationSignatures_st = areTheyUnique
					.listIterator(0);
			while (OperationSignatures_st.hasNext()) {
				outfile.write("\t\t\t" + OperationSignatures_st.next());
				if (OperationSignatures_st.hasNext()) {
					outfile.write(" | \n");
				} else {
					outfile.write(" ; \n");
				}
			}
		}
		

	}

	public static void createClassObjectSort(
			org.eclipse.uml2.uml.Package rootPackage) throws IOException {

		TreeIterator<EObject> treeIt = rootPackage.eAllContents();
		LinkedList<String> knownLifelines = new LinkedList<String>();
		outfile.newLine();

		outfile.write("sort ClassObject = struct");
		outfile.newLine();
		while (treeIt.hasNext()) {
			EObject el = treeIt.next();
			if (el.getClass().equals(LifelineImpl.class)) {
				if (!knownLifelines.contains(((LifelineImpl) el)
						.getRepresents().getName())) {
					knownLifelines.add(((LifelineImpl) el).getRepresents()
							.getName());
				}
			}
		}

		Iterator<String> it_knownObjects = knownLifelines.iterator();
		while (it_knownObjects.hasNext()) {
			outfile.write("\t\t\t" + it_knownObjects.next());

			if (it_knownObjects.hasNext())
				outfile.write(" | \n");
			else
				outfile.write(" ; \n");
		}
		outfile.newLine();
	}

	public static LinkedList<CollaborationImpl> getAllCollaborations(
			org.eclipse.uml2.uml.Package rootPackage) {
		TreeIterator<EObject> treeColab = rootPackage.eAllContents();
		LinkedList<CollaborationImpl> allColaborations = new LinkedList<CollaborationImpl>();
		while (treeColab.hasNext()) {
			EObject el = treeColab.next();
			if (el.getClass().equals(CollaborationImpl.class)) {
				allColaborations.add((CollaborationImpl) el);
				getAllInteractionsForCollaboration((CollaborationImpl) el);
			}
		}
		if (allColaborations.size() == 0) {
			System.err
					.println("Strange:there are no collaborations (SDs) in the UML model.\nPlease check for possible problems.");
		}
		return allColaborations;
	}

	public static LinkedList<Activity> getAllActivities(
			org.eclipse.uml2.uml.Package rootPackage) {
		LinkedList<Activity> activities = new LinkedList<Activity>();
		TreeIterator<EObject> treeActivities = rootPackage.eAllContents();
		while (treeActivities.hasNext()) {
			EObject el = treeActivities.next();
			if (el.getClass().equals(ActivityImpl.class)) {
				activities.add((ActivityImpl) el);
			}
		}
		return activities;
	}

	public static LinkedList<InteractionImpl> getAllInteractionsForCollaboration(
			CollaborationImpl collaboration) {
		TreeIterator<EObject> treeInteractions = collaboration.eAllContents();
		LinkedList<InteractionImpl> allInteractions = new LinkedList<InteractionImpl>();
		while (treeInteractions.hasNext()) {
			EObject el = treeInteractions.next();
			if (el.getClass().equals(InteractionImpl.class)) {
				allInteractions.add((InteractionImpl) el);
				getLifeLinesForInteraction((InteractionImpl) el);
				getFragmentsForInteraction((InteractionImpl) el);
			}
		}
		return allInteractions;
	}

	public static LinkedList<LifelineImpl> getLifeLinesForInteraction(
			InteractionImpl interaction) {
		TreeIterator<EObject> treeLifelines = interaction.eAllContents();
		LinkedList<LifelineImpl> allLifelines = new LinkedList<LifelineImpl>();
		readyProcessesPerLifeline = new HashMap<String, Stack<Process>>();
		busyProcessesPerLifeline = new HashMap<String, Stack<Process>>();
		// System.out.println("ENTIRE STACK:"+stackPerLifeline.entrySet().toString());
		while (treeLifelines.hasNext()) {
			EObject el = treeLifelines.next();
			if (el.getClass().equals(LifelineImpl.class)) {
				allLifelines.add((LifelineImpl) el);
				// System.out.print("Lifeline:"
				// + ((LifelineImpl) el).getRepresents().getName());
				// System.out.println("; type:"
				// + ((LifelineImpl) el).getRepresents().getType()
				// .getName());
				Process initProcess = new Process();
				Stack<Process> initStack = new Stack<Process>();
				initStack.push(initProcess);
				processes.add(initProcess);
				// System.out.println("Want to put:");
				// System.out.println("Key:"
				// + ((LifelineImpl) el).getRepresents().getName()
				// .toString());
				// System.out.println("Value:" + initStack.toString());
				readyProcessesPerLifeline.put(((LifelineImpl) el)
						.getRepresents().getName().toString(), initStack);
				busyProcessesPerLifeline.put(((LifelineImpl) el)
						.getRepresents().getName().toString(),
						new Stack<Process>());
				// System.out.println("ENTIRE STACK:"+stackPerLifeline.entrySet().toString());
			}
		}
		return allLifelines;
	}

	public static String getMessageArguments(Message message)
			throws ClassCastException {
		EList<ValueSpecification> arguments = message.getArguments();
		Iterator<ValueSpecification> arguments_iterator = arguments.iterator();
		StringBuffer args = new StringBuffer();
		boolean isFirst = true;
		int count = 0;
		while (arguments_iterator.hasNext()) {
			ValueSpecification argument = (ValueSpecification) arguments_iterator
					.next();
			if (isFirst)
				args.append("(");
			count++;
			isFirst = false;
			if (argument instanceof OpaqueExpression) {
				args.append(((OpaqueExpression) argument).getBodies().get(0)
						.replace("\"", ""));
				if (((OpaqueExpression) argument).getBodies().get(0)
						.startsWith("\"") // if argument is string
						&& !SortString.contains(((OpaqueExpression) argument)
								.getBodies().get(0).replace("\"", ""))) // No
																		// duplicate
																		// strings
					SortString.add(((OpaqueExpression) argument).getBodies()
							.get(0).replace("\"", ""));
			} else if (argument instanceof LiteralIntegerImpl) {
				args.append(argument.integerValue());
			} else if (argument instanceof LiteralBooleanImpl) {
				args.append(argument.booleanValue());
			} else if (argument instanceof LiteralStringImpl) {
				args.append(argument.stringValue());
				if (!SortString.contains(argument.stringValue()))
					SortString.add(argument.stringValue());
			} else {
				args.append("UnknownValue %FIXME");
				System.err.println("Could not determine argument value for "
						+ message.getName() + ":" + argument.getName());
			}
			if (count == arguments.size())
				args.append(")");
			else
				args.append(",");

		}

		return args.toString();
	}

	// in synch_call_send and synch_reply_receive we have (id,
	// Class,obj,methodName..) so we need
	// the class and object to pass

	// synch_call_send
	public static String getClassAndObjectForMCB(Message message) {
		StringBuffer args = new StringBuffer();
		if (message.getReceiveEvent() == null) {
			System.err.println("Problem:Message " + message.getName()
					+ " has no ReceiveEvent.");
			System.err.println("Check the UML model for inconsistencies.");
			System.exit(1);
		}
		// if we're dealing with a gate
		if (message.getReceiveEvent().getClass()
				.equals(org.eclipse.uml2.uml.internal.impl.GateImpl.class)) {
			GateImpl actualGateCalling = (GateImpl) message.getReceiveEvent();
			// we neeed the owner fragment InteractionUse that will tell us
			// which lifeline it covers
			Element ownerClass = ((org.eclipse.uml2.uml.Element) actualGateCalling)
					.getOwner();
			if (ownerClass == null) {
				System.err.println("Problem:Message " + message.getName()
						+ "has no actual gate linked to the formal one!");
				System.err.println("Check the UML model for inconsistencies.");
				System.exit(1);
			}
			InteractionFragment ownerFragment = (InteractionFragment) ownerClass;
			args.append(ownerFragment.getCovered(null).getRepresents()
					.getType().getName()
					+ ","
					+ ownerFragment.getCovered(null).getRepresents().getName()
					+ ",");
		} else if (message.getReceiveEvent().getClass()
				.equals(MessageOccurrenceSpecificationImpl.class)) {
			InteractionFragment event = (MessageOccurrenceSpecificationImpl) message
					.getReceiveEvent();

			args.append(event.getCovered(null).getRepresents().getType()
					.getName()
					+ ","
					+ event.getCovered(null).getRepresents().getName()
					+ ",");
		}
		return args.toString();
	}

	// synch_reply_receive
	public static String getClassAndObjectForMCE(Message message) {
		StringBuffer args = new StringBuffer();
		if (message.getSendEvent() == null) {
			System.err.println("Problem:Message " + message.getName()
					+ " has no SendEvent.");
			System.err.println("Check the UML model for inconsistencies.");
			System.exit(1);
		}
		// if we're dealing with a gate
		if (message.getSendEvent().getClass()
				.equals(org.eclipse.uml2.uml.internal.impl.GateImpl.class)) {
			GateImpl actualGateReceiving = (GateImpl) message.getSendEvent();
			Element ownerClass = ((Element) actualGateReceiving).getOwner();
			if (ownerClass == null) {
				System.err.println("Problem:Message " + message.getName()
						+ "has no actual gate linked to the formal one!");
				System.err.println("Check the UML model for inconsistencies.");
				System.exit(1);
			}
			InteractionFragment ownerFragment = (InteractionFragment) ownerClass;
			args.append(ownerFragment.getCovered(null).getRepresents()
					.getType().getName()
					+ ","
					+ ownerFragment.getCovered(null).getRepresents().getName()
					+ ",");

		} else if (message
				.getSendEvent()
				.getClass()
				.equals(org.eclipse.uml2.uml.internal.impl.MessageOccurrenceSpecificationImpl.class)) {

			InteractionFragment event = (MessageOccurrenceSpecificationImpl) message
					.getSendEvent();

			args.append(event.getCovered(null).getRepresents().getType()
					.getName()
					+ ","
					+ event.getCovered(null).getRepresents().getName()
					+ ",");
		}

		return args.toString();
	}

	public static void extractFragments(
			Collection<InteractionFragment> fragments,
			InteractionOperand operand, String operator, boolean firstOperand,
			boolean lastOperand, String guard, boolean insideOperand) {

		Iterator<InteractionFragment> fragments_iterator = fragments.iterator();

		boolean firstSendFound = false;
		Process responsibleProcess = null;
		LoopProcess loopProcess = null;
		Stack<Process> theReadyStack = null;
		Stack<Process> theBusyStack = null;
		while (fragments_iterator.hasNext()) {
			InteractionFragment el = (InteractionFragment) fragments_iterator
					.next();
			if (el.getClass().equals(MessageOccurrenceSpecificationImpl.class)) {
				// System.out.println("========");
				// System.out.print("MessageOccurence:"
				// + ((InteractionFragmentImpl) el).getCovered(null)
				// .getRepresents().getName());
				// System.out.print("; represents:"
				// + ((InteractionFragmentImpl) el).getCovered(null)
				// .getRepresents().getType().getName());
				// System.out.println("; Message:"
				// + ((MessageOccurrenceSpecificationImpl) el)
				// .getMessage().getName());

				String arguments = getMessageArguments(((MessageOccurrenceSpecificationImpl) el)
						.getMessage());

				MessageOccurrenceSpecificationImpl occurence = (MessageOccurrenceSpecificationImpl) el;
				Event event = occurence.getEvent();
				// 6 cases:
				// 1. SendOccurrence synchCall
				// 2. SendOccurrence reply
				// 3. ReceiveOccurrence synchCall
				// 4. ReceiveOccurrence reply
				// 5. ReceiveOccurrence asynchCall
				// 6. SendOccurrence asynchCall
				// System.out.println("sort:"

				theReadyStack = readyProcessesPerLifeline
						.get(((InteractionFragment) el).getCovered(null)
								.getRepresents().getName());
				theBusyStack = busyProcessesPerLifeline
						.get(((InteractionFragment) el).getCovered(null)
								.getRepresents().getName());
				// System.out.println("Ready stack BEFORE message occurence:"
				// + theReadyStack.toString());
				// System.out.println("Busy stack BEFORE message occurence:"
				// + theBusyStack.toString());

				if (occurence.getMessage().getSendEvent() == el) {
					// System.out.print("DIRECTION; sending; ");
					SendOperationEvent sendEvent = (SendOperationEvent) event;
					// System.out.println("Original operation:"
					// + sendEvent.getOperation().getName());
					if (occurence.getMessage().getMessageSort().toString()
							.equals("synchCall")) {
						// Case 1:

						Process theProcess = (Process) theReadyStack.pop();
						theProcess.setEnclosingInteraction(el
								.getEnclosingInteraction());

						if (!firstSendFound && insideOperand) {
							responsibleProcess = theProcess;
							if (operator.equals("alt")) {
								if (firstOperand)
									responsibleProcess.addBeginAltFragment();
								if (!guard.equals("else"))
									responsibleProcess.addAltFragment(guard);
								else
									responsibleProcess.addBeginAltFragment();
							}

							else if (operator.equals("opt")) {
								responsibleProcess.addOptFragment(guard);
							} else if (operator.equals("break")) {
								responsibleProcess.addBreakFragment(guard);
							} else if (operator.equals("loop")) {
								StringBuffer paramsToPass = new StringBuffer();
								StringBuffer signatureParams = new StringBuffer();
								for (Map.Entry<String, String> entry : responsibleProcess
										.getSumParameters().entrySet()) {
									paramsToPass.append("," + entry.getKey());
									signatureParams.append("," + entry.getKey()
											+ ":" + entry.getValue());
								}

								responsibleProcess
										.addCallLoopFragment(responsibleProcess.operationImpl
												.getName()
												+ "_loop"
												+ loopCounter
												+ "(id,obj"
												+ paramsToPass.substring(0,
														paramsToPass.length())
												+ ")");
								theReadyStack.push(theProcess); // push it back
																// to ready
																// again, since
																// it's only a
																// call to
																// "_loop(id)."
								// no synch_call_send and synch_reply_receive

								loopProcess = new LoopProcess();
								loopProcess
										.setCallSignature(responsibleProcess.operationImpl
												.getName()
												+ "_loop"
												+ loopCounter
												+ "(id,obj"
												+ paramsToPass.substring(0,
														paramsToPass.length())
												+ ")"); // call signature is the
														// last thing inside the
														// loop
														// .loop_repeatAgain(...)
														// <> internal

								loopProcess
										// this is the process signature
										.addLoopSignature(responsibleProcess.operationImpl
												.getName()
												+ "_loop"
												+ loopCounter
												+ "(id:Nat,obj:ClassObject"
												+ signatureParams.substring(0,
														signatureParams
																.length())
												+ ")");
								loopCounter++;
								loopProcess
										.setOperationImpl(responsibleProcess.operationImpl);
								loopProcess.addCondition(guard);
								loopProcess.setEnclosingInteraction(el
										.getEnclosingInteraction());
								processes.add(loopProcess);
								//
								// now, should set the _loop Process as the
								// active one on the lifeline
								// so that all the calls below will go there
								theProcess = loopProcess;
							}

							firstSendFound = true;
						}

						String classAndobject = getClassAndObjectForMCB(((MessageOccurrenceSpecificationImpl) el)
								.getMessage());

						theProcess.addInvocation("synch_call_send(id,"
								+ classAndobject
								+ ((MessageOccurrenceSpecificationImpl) el)
										.getMessage().getName() + arguments
								+ ")");

						// now push it to busy
						theBusyStack = busyProcessesPerLifeline
								.get(((InteractionFragment) el)
										.getCovered(null).getRepresents()
										.getName());
						theBusyStack.push(theProcess);

					} else if (occurence.getMessage().getMessageSort()
							.toString().equals("reply")) {
						// Case 2:
						theReadyStack = readyProcessesPerLifeline
								.get(((InteractionFragment) el)
										.getCovered(null).getRepresents()
										.getName());
						Process theProcess = (Process) theReadyStack.pop();
						theProcess.setEnclosingInteraction(el
								.getEnclosingInteraction());

						if (!theProcess.isProcessed) {
							theProcess.addInvocation("synch_reply_send(id,"
									+ ((InteractionFragment) el)
											.getCovered(null).getRepresents()
											.getType().getName()
									+ ",obj,"
									+ ((MessageOccurrenceSpecificationImpl) el)
											.getMessage().getName() + "_return"
									+ arguments + ")");
							theProcess.setProcessed();
						}

					} else if (occurence.getMessage().getMessageSort()
							.toString().equals("asynchCall")) {
						// Case 6: asynch send
						String classAndobject = getClassAndObjectForMCB(((MessageOccurrenceSpecificationImpl) el)
								.getMessage());
						Process theProcess = (Process) theReadyStack.peek();
						theProcess.addInvocation("asynch_call_send(id,"
								+ classAndobject
								+ ((MessageOccurrenceSpecificationImpl) el)
										.getMessage().getName() + arguments
								+ ")");
						theProcess.setEnclosingInteraction(el
								.getEnclosingInteraction());
					}

				} else if (occurence.getMessage().getReceiveEvent() == el) {
					// System.out.println("DIRECTION; receiving...");
					ReceiveOperationEvent receiveEvent = (ReceiveOperationEvent) event;
					// System.out.println("Original operation:"
					// + receiveEvent.getOperation().getName());

					if (occurence.getMessage().getMessageSort().toString()
							.equals("synchCall")) {
						// Case 3:
						Operation opCheck = (Operation) receiveEvent
								.getOperation();
						org.eclipse.uml2.uml.Class classCheck = (org.eclipse.uml2.uml.Class) ((InteractionFragment) el)
								.getCovered(null).getRepresents().getType();

						Process findProcess = findProcess(classCheck, opCheck);

						if (findProcess == null) {
							findProcess = new Process(classCheck, opCheck);
							processes.add(findProcess);
						}
						if (!findProcess.isProcessed) {
							findProcess.setEnclosingInteraction(el
									.getEnclosingInteraction());

							StringBuffer changedStep = new StringBuffer();
							StringBuffer changedParams = new StringBuffer();
							changedStep.append("sum ");
							for (Map.Entry<String, String> entry : findProcess
									.getOpParametersIn().entrySet()) {
								String key = entry.getKey();
								String value = entry.getValue();
								changedStep.append(key + ":" + value + ",");
								changedParams.append(key + ",");
								findProcess.addSumParameter(key, value);
							}
							changedStep.append("obj:ClassObject.");

							if (findProcess.getOpParametersIn().size() != 0) { // there
																				// are
																				// method
																				// arguments
								findProcess
										.addInvocation(changedStep
												+ "synch_call_receive(id,"
												+ ((InteractionFragment) el)
														.getCovered(null)
														.getRepresents()
														.getType().getName()
												+ ",obj,"
												+ ((MessageOccurrenceSpecificationImpl) el)
														.getMessage().getName()
												+ "("
												+ changedParams
														.substring(
																0,
																changedParams
																		.length() - 1)
												+ ")" + ")");
							} else { // no method arguments
								findProcess
										.addInvocation(changedStep
												+ "synch_call_receive(id,"
												+ ((InteractionFragment) el)
														.getCovered(null)
														.getRepresents()
														.getType().getName()
												+ ",obj,"
												+ ((MessageOccurrenceSpecificationImpl) el)
														.getMessage().getName()
												+ ")");
							}

						}
						theReadyStack = readyProcessesPerLifeline
								.get(((InteractionFragment) el)
										.getCovered(null).getRepresents()
										.getName());
						theReadyStack.push(findProcess);
						// System.out.println("!!!!Stack:" +
						// theStack.toString());

					} else if (occurence.getMessage().getMessageSort()
							.toString().equals("reply")) {
						// Case 4:
						// this is for replies from other messages, when the
						// process has called someone

						// find last busy process, pop it
						theBusyStack = busyProcessesPerLifeline
								.get(((InteractionFragment) el)
										.getCovered(null).getRepresents()
										.getName());
						Process findProcess = (Process) theBusyStack.pop();
						findProcess.setEnclosingInteraction(el
								.getEnclosingInteraction());

						// push it to ready now
						theReadyStack = readyProcessesPerLifeline
								.get(((InteractionFragment) el)
										.getCovered(null).getRepresents()
										.getName());
						theReadyStack.push(findProcess);

						String classAndobject = getClassAndObjectForMCE(((MessageOccurrenceSpecificationImpl) el)
								.getMessage());

						// for synch_reply_receive we need the sum: ...
						// parameters,
						// which
						// can be obtained only if we know the called process
						// OpParametersReturn dictionary
						Process calledProcess = findProcess(
								classAndobject.split(",")[0],
								((MessageOccurrenceSpecificationImpl) el)
										.getMessage().getName());
						// preparing for prepending "sum param:Type,..." and
						// appending arguments
						StringBuffer sumParameters = new StringBuffer();
						StringBuffer appendedParameters = new StringBuffer();
						sumParameters.append("sum ");
						// adding parameters TODO: the names should NOT be from the operation
						EList<ValueSpecification> argumentNames = (((MessageOccurrenceSpecificationImpl) el)
								.getMessage().getArguments());
						Iterator<ValueSpecification> arguments_iterator = null;
						LinkedList<String> argNames = new LinkedList<String>();
						if(!argumentNames.isEmpty()){
							arguments_iterator = argumentNames.iterator();
							while (arguments_iterator.hasNext()) {
								ValueSpecification argument = (ValueSpecification) arguments_iterator.next();
								argNames.add(((OpaqueExpression) argument).getBodies().get(0));
							}
						}
						
						
						// added
						int i = 0;
						for (Map.Entry<String, String> entry : calledProcess
								.getOpParametersReturn().entrySet()) {
							
//							ValueSpecification argument = (ValueSpecification) arguments_iterator.next();
//							String argName = ((OpaqueExpression) argument).getBodies().get(0);
							StringBuffer argName = new StringBuffer("ebise");
							if(argNames.size()!=0){
								 argName = new StringBuffer(argNames.get(i++));

							}
							String key = entry.getKey();
							String value = entry.getValue();
							sumParameters.append(argName + ":" + value + ",");
							appendedParameters.append(argName + ",");
							findProcess.addSumParameter(argName.toString(), value);
						}

						if (calledProcess.getOpParametersReturn().size() != 0) {
							// prepending the "sum param:Type.."
							// adding the parameter values in the call as well
							// at the end
							findProcess.addInvocation(sumParameters.substring(
									0, sumParameters.length() - 1)
									+ ".synch_reply_receive(id,"
									+ classAndobject
									+ ((MessageOccurrenceSpecificationImpl) el)
											.getMessage().getName()
									+ "_return"
									+  arguments + ")"); // TODO: remove arguments, was there just to check!

						} // no parameters to add
						else {
							findProcess.addInvocation("synch_reply_receive(id,"
									+ classAndobject
									+ ((MessageOccurrenceSpecificationImpl) el)
											.getMessage().getName() + "_return"
									+ ")");

						}

						// System.out.println("!!!!Stack:" +
						// theStack.toString());
					} else if (occurence.getMessage().getMessageSort()
							.toString().equals("asynchCall")) {

						// Case 5: // asynchronous receive
						Operation opCheck = (Operation) receiveEvent
								.getOperation();
						org.eclipse.uml2.uml.Class classCheck = (org.eclipse.uml2.uml.Class) ((InteractionFragment) el)
								.getCovered(null).getRepresents().getType();

						Process findProcess = findProcess(classCheck, opCheck);

						if (findProcess == null) {
							findProcess = new Process(classCheck, opCheck);
							processes.add(findProcess);
						}
						if (!findProcess.isProcessed) {
							findProcess.setEnclosingInteraction(el
									.getEnclosingInteraction());

							StringBuffer changedStep = new StringBuffer();
							StringBuffer changedParams = new StringBuffer();
							changedStep.append("sum ");
							for (Map.Entry<String, String> entry : findProcess
									.getOpParametersIn().entrySet()) {
								String key = entry.getKey();
								String value = entry.getValue();
								changedStep.append(key + ":" + value + ",");
								changedParams.append(key + ",");
								findProcess.addSumParameter(key, value);
							}

							changedStep.append("obj:ClassObject.");
							if (findProcess.getOpParametersIn().size() != 0) { // there
								// are
								// method
								// arguments
								findProcess
										.addInvocation(changedStep
												+ "asynch_call_receive(id,"
												+ ((InteractionFragment) el)
														.getCovered(null)
														.getRepresents()
														.getType().getName()
												+ ",obj,"
												+ ((MessageOccurrenceSpecificationImpl) el)
														.getMessage().getName()
												+ "("
												+ changedParams
														.substring(
																0,
																changedParams
																		.length() - 1)
												+ ")" + ")");
							} else {
								findProcess
										.addInvocation(changedStep
												+ "asynch_call_receive(id,"
												+ ((InteractionFragment) el)
														.getCovered(null)
														.getRepresents()
														.getType().getName()
												+ ",obj,"
												+ ((MessageOccurrenceSpecificationImpl) el)
														.getMessage().getName()
												+ ")");
							}

						}
						theReadyStack = readyProcessesPerLifeline
								.get(((InteractionFragment) el)
										.getCovered(null).getRepresents()
										.getName());
						theReadyStack.push(findProcess);
						// System.out.println("!!!!Stack:" +
						// theStack.toString());

					}

				}
				// System.out.println("; Message event:"+((MessageOccurrenceSpecificationImpl)el).getMessage().getSendEvent());

				// System.out.println("Ready stack AFTER message occurence:"
				// + theReadyStack.toString());
				// System.out.println("Busy stack AFTER message occurence:"
				// + theBusyStack.toString());

			} else if (el.getClass().equals(CombinedFragmentImpl.class)) {
				getOperandsForCombinedFragment((CombinedFragmentImpl) el,
						((CombinedFragmentImpl) el).getInteractionOperator()
								.toString());
				// System.out.println("EndCombinedFragment");
			}
		}
		if (insideOperand) {

			if (operator.equals("alt")) {

				responsibleProcess.addEndAltFragment(lastOperand);
				if (lastOperand)
					if (guard.equals("else"))
						responsibleProcess.addCloseFragment(false);
					else
						responsibleProcess.addCloseFragment(true);
			} else if (operator.equals("opt")) {
				responsibleProcess.closeOptFragment();

			} else if (operator.equals("break")) {
				responsibleProcess.addCloseBreakFragment();

			} else if (operator.equals("loop")) {
				// remove the ready _loop process from the queue in order to
				// keep synch_reply_send appearing
				// in the right process
				theReadyStack.pop();
			}

		}

	}

	public static Collection<InteractionFragment> getFragmentsForInteraction(
			InteractionImpl interaction) {
		Collection<InteractionFragment> fragments = EcoreUtil.getObjectsByType(
				interaction.eContents(),
				UMLPackage.Literals.INTERACTION_FRAGMENT);
		if (fragments != null)
			extractFragments(fragments, null, null, false, false, null, false);
		else {
			System.err
					.println("Strange:No Fragments found inside the model, for SD="
							+ interaction.getName());
			System.err
					.println("This means no communication between objects at all. Check model for consistencies.");
		}
		return fragments;
	}

	public static EList<InteractionOperand> getOperandsForCombinedFragment(
			CombinedFragment fragment, String operator) {
		EList<InteractionOperand> operands = fragment.getOperands();
		if (operands == null) {
			System.err
					.println("Strange:No Operands found inside CombinedFragment "
							+ operator);
		}
		Iterator<InteractionOperand> operands_iterator = operands.iterator();
		boolean firstOperand = true;
		boolean lastOperand = false;
		int countThem = 0;
		while (operands_iterator.hasNext()) {
			InteractionOperand operand = (InteractionOperand) operands_iterator
					.next();
			OpaqueExpressionImpl opaqueExpression = (OpaqueExpressionImpl) operand
					.getGuard().getSpecification();
			countThem++;
			if (countThem == operands.size())
				lastOperand = true;
			getFragmentsInsideOperand(operand, operator, firstOperand,
					lastOperand, opaqueExpression.getBodies().get(0).toString()); // TODO: what if there is no body??
			firstOperand = false;
		}
		return operands;
	}

	/**
	 * @param operand
	 * @param operator
	 * @param firstOperand
	 * @param lastOperand
	 * @param guard
	 * @return
	 */
	public static Collection<InteractionFragment> getFragmentsInsideOperand(
			InteractionOperand operand, String operator, boolean firstOperand,
			boolean lastOperand, String guard) {
		Collection<InteractionFragment> fragments = EcoreUtil.getObjectsByType(
				operand.eContents(), UMLPackage.Literals.INTERACTION_FRAGMENT);

		extractFragments(fragments, operand, operator, firstOperand,
				lastOperand, guard, true); // true flag for insideOperand

		return fragments;
	}

	public static boolean checkExists(Process process) {
		ListIterator<Process> iterator = processes.listIterator(0);
		boolean exists = false;
		while (iterator.hasNext()) {
			Process p = (Process) iterator;
			if (p.equals(process)) {
				exists = true;
				break;
			}
		}
		return exists;
	}

	public static Process findProcess(org.eclipse.uml2.uml.Class classImpl,
			Operation operationImpl) {
		Process proc = null;
		ListIterator<Process> iterator = processes.listIterator(0);
		while (iterator.hasNext()) {
			Process p = (Process) iterator.next();
			if (p.classImpl == classImpl && p.operationImpl == operationImpl) {
				proc = p;
				break;
			}
		}
		return proc;
	}

	//via class and operation names
	public static Process findProcess(String className,
			String operationName) {
		Process proc = null;
		ListIterator<Process> iterator = processes.listIterator(0);
		while (iterator.hasNext()) {
			Process p = (Process) iterator.next();
			if (p.equalsClassNameANDOperationName(className, operationName)) {
				proc = p;
				break;
			}
		}
		return proc;
	}

	public static void createActionDefinitions() throws IOException {
		outfile.write("\n %-------action definitions------- \n");
		outfile.write("act synch_call_send,synch_call_receive:Nat#ClassType#ClassObject#Method; \n");
		outfile.write("act synch_reply_receive,synch_reply_send:Nat#ClassType#ClassObject#Method; \n \n");
		outfile.write("act synch_call:Nat#ClassType#ClassObject#Method; \n");
		outfile.write("act synch_reply:Nat#ClassType#ClassObject#Method; \n \n");
		outfile.write("act asynch_call_send,asynch_call_receive:Nat#ClassType#ClassObject#Method; \n");
		outfile.write("act asynch_call:Nat#ClassType#ClassObject#Method; \n \n");
		outfile.write("act internal; \n");
		outfile.write("%-------end action definitions------- \n");
	}

	public static String determinePrimitiveType(PrimitiveTypeImpl typearg) {
		// System.out.println("typeArg:"+typearg+";"+typearg.eIsProxy());
		if (typearg.eIsProxy()) {
			if (typearg.eProxyURI().toString().contains("Integer"))
				return "Int";
			else if (typearg.eProxyURI().toString().contains("Boolean"))
				return "Bool";
			else if (typearg.eProxyURI().toString().contains("Natural"))
				return "Nat";
			else if (typearg.eProxyURI().toString().contains("String"))
				return "SortString";
			else
				return "Unknown %FIXME";
		} else {
			if (typearg.getName() != null) {
				String nameOfArg = typearg.getName().toString();
				if (nameOfArg.contains("float") || nameOfArg.contains("double"))
					return "Real";
				else if (nameOfArg.contains("long")
						|| nameOfArg.contains("short")
						|| nameOfArg.contains("byte"))
					return "Int";
				else if (nameOfArg.contains("char"))
					return "SortString";
			} else
				return "Unknown %FIXME";
		}
		return "Unknown %FIXME";
	}

	public static Collection<org.eclipse.uml2.uml.Class> getClasses(
			org.eclipse.uml2.uml.Package rootPackage) {
		return EcoreUtil.getObjectsByType(rootPackage.eContents(),
				UMLPackage.Literals.CLASS);

	}

	public static Collection<Operation> getOperations(
			org.eclipse.uml2.uml.Class classarg) {
		return EcoreUtil.getObjectsByType(classarg.eContents(),
				UMLPackage.Literals.OPERATION);
	}

	public static Collection<Parameter> getParameters(Operation operationarg) {
		return EcoreUtil.getObjectsByType(operationarg.eContents(),
				UMLPackage.Literals.PARAMETER);
	}

	public static String stripProcessName(String step) {
		String delims = "(),";
		String[] parts = step.split(delims);
		return parts[1] + "_" + parts[3].split("\\)")[0].split("\\(")[0];
	}

	public static void printProcessesMCRL2() {
		ListIterator<Process> processes_iterator = processes.listIterator(0);
		while (processes_iterator.hasNext()) {
			Process process = (Process) processes_iterator.next();
			String processSignature = null;
			if (process.getClassImpl() != null
					&& process.getOperationImpl() != null) { // a regular
																// process
				processSignature = process.classImpl.getName() + "_"
						+ process.operationImpl.getName();
			}

			else if (process.getClassImpl() == null // these are the starting
													// points for traversal
					&& process.getOperationImpl() == null
					&& !process.getInvocations().isEmpty()) {
				processSignature = process.getEnclosingInteraction().getName();
			}
			if (processSignature != null) {
				System.out.println("\nProcess:" + processSignature);
				LinkedList<String> steps = new LinkedList<String>();
				LinkedList<String> invocations = process.getInvocations();
				Iterator<String> it_invocations = invocations.iterator();
				while (it_invocations.hasNext()) {
					String step = (String) it_invocations.next();
					if (step.contains("synch_call_send")
							|| step.contains("asynch_call_send")) {
						System.out.println("STEP:" + step);
						step = stripProcessName(step);
						if (!steps.contains(step))
							steps.add(step);
					}
				}
				starters.put(processSignature, steps);
			}
		}
	}

	public static LinkedList<String> buildCallChain(String processSignature) {
		try {
			LinkedList<String> chain = starters.get(processSignature);
			Stack<String> stack = new Stack<String>();
			stack.addAll(chain);
			while (!stack.isEmpty()) {
				String procSig = stack.pop();
				LinkedList<String> steps = starters.get(procSig);
				for (String step : steps) {
					if (!chain.contains(step) && !stack.contains(step)) {
						stack.push(step);
						chain.add(step);
					}
				}
			}
			return chain;

		} catch (NullPointerException e) {
			System.err.println("No such process!");
			return null;
		}
	}

	public static void processNode(ActivityNode node) {
		// because of mCRL2's restriction
		// "Parallel operator cannot occur in the scope of pCRL operators"
		// currently we only take into account CallBehaviorAction and the
		// edge-weight leading to it (incoming)
		Collection<ActivityEdge> edges = node.getIncomings();
		if (edges.size() != 1) {
			System.err
					.println("None, or more than one outgoing edge for CallBehaviorAction.\nNot sure which one to take...Please correct this");
			System.exit(1);
		}

		Iterator<ActivityEdge> edge_iterator = edges.iterator();
		ActivityEdge edge = edge_iterator.next();
		int weight = 1;
		if (edge.getWeight() != null)
			weight = edge.getWeight().integerValue();
		LinkedList<String> processesChain = buildCallChain(((CallBehaviorAction) node)
				.getBehavior().getName());
		for (int i = 1; i <= weight; i++) {
			systemInit.append(((CallBehaviorAction) node).getBehavior()
					.getName() + "(" + i + ")" + " || \n");
			for (String proc : processesChain)
				systemInit.append(proc + "(" + i + ") || % "
						+ ((CallBehaviorAction) node).getBehavior().getName()
						+ "\n");
		}
	}

	public static void main(String args[]) {
		try{
			// int starter = 0;
			EPackage.Registry.INSTANCE
					.put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
					UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
			ResourceSet resourceSet = new ResourceSetImpl();
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put("xmi", new XMIResourceFactoryImpl());
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put("xml", new XMLResourceFactoryImpl());
			resourceSet.getPackageRegistry().put(
					"http://schema.omg.org/spec/UML/2.1", UMLPackage.eINSTANCE);
			resourceSet.getPackageRegistry().put(
					"http://schema.omg.org/spec/UML/2.2", UMLPackage.eINSTANCE);

			Resource resource = null;
				File f = new File(args[0]);
				URI uri = URI.createFileURI(f.getAbsolutePath());
				resource = (UMLResource) resourceSet.getResource(uri, true);
				resource.load(null);
				org.eclipse.uml2.uml.Package rootPackage = (org.eclipse.uml2.uml.Package) EcoreUtil
						.getObjectByType(resource.getContents(),
								UMLPackage.Literals.PACKAGE);
				// ---------------------------
				if(rootPackage==null){
					System.err.println("Root package of the UML model is missing. Please check your model and .uml export!");
					System.err.println("Exiting....");
					System.exit(1);
				}
				createSorts(rootPackage);
				createClassObjectSort(rootPackage);
				outfile.newLine();

				getAllCollaborations(rootPackage);
				outfile.newLine();
				outfile.newLine();
				createSortStrings();
				createActionDefinitions();

				outfile.newLine();
				outfile.write("%-------Process definitions--------");
				outfile.newLine();
				ListIterator<Process> processes_iterator = processes
						.listIterator(0);
				// System.out.println("PROCESSES:");
				while (processes_iterator.hasNext()) {
					Process process = (Process) processes_iterator.next();
					if (!process.getInvocations().isEmpty()) {// those with no
																// invocations are
																// surplus, the rest
																// are
																// initiators
						StringBuffer processString = process.prepareForMCRL2();
						outfile.append(processString.toString());
					}
				}
				printProcessesMCRL2();
				outfile.write("%-------End process definitions-------- \n \n");
				// systemInit construction via ADs
				outfile.write("%-------systemInitProcess-------- \n \n");
				int indexActivity = 0;

				LinkedList<Activity> activities = getAllActivities(rootPackage);
				if (activities.size() == 0) {
					System.err
							.println("No activity diagram found!. Please specify one for the system-level concurrency");
					System.exit(1);
				}

				if (activities.size() > 1) {
					if (args[1] == null
							|| !args[1].contains("-setup")
							|| rootPackage
									.getPackagedElement(args[1].split("=")[1]) == null) {
						System.err
								.println("More than one Activity Diagram present:");
						for (Activity activity : activities)
							System.err.println("\t AD name: " + activity.getName());
						System.err
								.println("Please specify the correct one for the system-level concurrency setup,\nusing the -setup=<ActivityName> switch");
						System.exit(1);
					} else {
						indexActivity = activities.indexOf(rootPackage
								.getPackagedElement(args[1].split("=")[1]));
					}

				}

				for (ActivityNode node : activities.get(indexActivity).getNodes())
					if (node instanceof CallBehaviorAction)
						processNode(node);
				outfile.write(systemInit.substring(0, systemInit.lastIndexOf("||"))
						+ "; \n");

				outfile.write("%-------Init section-------- \n");
				createInitSection();
				outfile.close();
				System.out.println("------------------------------");
				System.out.println("Finished generating model <model.mcrl2>");
		} catch(ArrayIndexOutOfBoundsException me){
			System.err.println("Usage: UML2mCRL2 <exportedModel.uml>");
			System.exit(1);
		}
		catch (Exception e) {
			System.err.println("Usage: UML2mCRL2 <exportedModel.uml>");
			System.err.println("Error message: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

	}

	public static void createSortStrings() throws IOException {
		outfile.newLine();
		ArrayList<String> passedStrings = new ArrayList<String>();
//		System.out.println("Debug SortString: " + SortString);
		if (SortString.size() != 0) {
			outfile.write("sort SortString = struct \n");
			Iterator<String> it_sortString = SortString.iterator();
			while (it_sortString.hasNext()) {
				String tmpString = (String) it_sortString.next();
				if (!passedStrings.contains(tmpString))
					outfile.write("\t\t\t " + tmpString + "");
				if (it_sortString.hasNext()) {
					outfile.write(" | \n");
				} else {
					outfile.write(" ; \n");
				}
				passedStrings.add(tmpString);
			}
		}
		outfile.write("%-------end sorts--------- \n");
	}

	public static void createInitSection() throws IOException {
		outfile.newLine();
		outfile.write("init hide ({internal}, \n");
		outfile.write("allow({internal,synch_call,synch_reply, asynch_call \n");
		outfile.write("}, \n");
		outfile.write("comm({ \n");
		outfile.write("synch_call_send|synch_call_receive->");
		outfile.write("synch_call, \n");
		outfile.write("synch_reply_receive|synch_reply_send->  \n");
		outfile.write("synch_reply,  \n");
		outfile.write("asynch_call_send|asynch_call_receive->  \n");
		outfile.write("asynch_call  \n");
		outfile.write("},  \n");
		outfile.write("\t\t\t\t systemInit  \n");
		outfile.write(")));");
	}
}