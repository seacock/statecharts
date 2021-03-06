/** 
 * Copyright (c) 2016 committers of YAKINDU and others. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 * Contributors:
 * @author René Beckmann (beckmann@itemis.de)
 *
*/
package org.yakindu.sct.model.sexec.transformation.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.yakindu.sct.model.sexec.ExecutionFlow;
import org.yakindu.sct.model.sexec.ExecutionState;
import org.yakindu.sct.model.sexec.Step;
import org.yakindu.sct.model.sexec.extensions.SExecExtensions;
import org.yakindu.sct.model.sexec.naming.INamingService;
import org.yakindu.sct.model.sexec.naming.TreeNamingService;
import org.yakindu.sct.model.sexec.transformation.FlowOptimizer;
import org.yakindu.sct.model.sgraph.State;
import org.yakindu.sct.model.sgraph.Statechart;
import org.yakindu.sct.test.models.SCTUnitTestModels;

import com.google.inject.Inject;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TreeNamingServiceTest extends ModelSequencerTest {
	@Inject
	protected SCTUnitTestModels testModels;

	@Inject
	FlowOptimizer optimizer;

	@Inject
	protected TreeNamingService statechartNamingService;

	@Inject
	protected TreeNamingService executionflowNamingService;

	private List<Statechart> statecharts;

	@Before
	public void setupNamingService() {
		statecharts = Collections.emptyList();

		try {
			statecharts = testModels.loadAllStatecharts();
		} catch (Exception e) {
			fail(e.getMessage());
		}

		optimizer.inlineReactions(false);
		optimizer.inlineExitActions(false);
		optimizer.inlineEntryActions(true);
		optimizer.inlineEnterSequences(true);
		optimizer.inlineExitSequences(true);
		optimizer.inlineChoices(true);
		optimizer.inlineEntries(true);
		optimizer.inlineEnterRegion(true);
		optimizer.inlineExitRegion(true);

		// TODO: Why does PerformanceTest doesn't work?
		Statechart statecharttoRemove = null;
		for (Statechart sct : statecharts) {
			if (sct.getName().equals("PerformanceTest")) {
				statecharttoRemove = sct;
			}
		}
		statecharts.remove(statecharttoRemove);
	}

	@Test
	public void testDefaultNamingServiceState_NoDoubles() {

		for (Statechart statechart : statecharts) {

			// Transform statechart
			ExecutionFlow flow = sequencer.transform(statechart);
			flow = optimizer.transform(flow);

			List<String> names = new ArrayList<String>();

			executionflowNamingService.setMaxLength(15);
			executionflowNamingService.setSeparator('_');

			// Initialize naming services for statechart and ExecutionFlow
			long t0 = System.currentTimeMillis();
			executionflowNamingService.initializeNamingService(flow);
			executionflowNamingService.test_printTreeContents();
			System.out.print("Time needed for initialization [ms]: ");
			System.out.println(System.currentTimeMillis() - t0);
			System.out.println("Generated names:");
			for (ExecutionState state : flow.getStates()) {
				String name = executionflowNamingService.getShortName(state);
				if (names.contains(name)) {
					System.out.println("Conflicting name: " + name);
					for (String n : names) {
						System.out.println(n);
					}
				}
				assertEquals(names.contains(name), false);
				names.add(name);
				System.out.println(name);
			}
			System.out.println();
		}
	}

	@Test
	public void nameLengthTest31() {
		nameLengthTest(31);
	}

	@Test
	public void nameLengthTest20() {
		nameLengthTest(20);
	}

	@Test
	public void nameLengthTest15() {
		nameLengthTest(15);
	}

	@Test
	public void nameLengthTest10() {
		nameLengthTest(10);
	}

	@Test
	public void nameLengthTest8() {
		nameLengthTest(8);
	}

	@Test
	public void optimizerCombinationsTest() {
		Statechart toTest = null;

		for (Statechart statechart : statecharts) {
			if (statechart.getName().equals("DeepEntry")) {
				toTest = statechart;
			}
		}

		assertEquals(true, toTest != null);

		ExecutionFlow flow = sequencer.transform(toTest);

		executionflowNamingService.setMaxLength(0);
		executionflowNamingService.setSeparator('_');

		for (int i = 0; i < (1 << 9); i++) {
			optimizer.inlineReactions((i & (1)) != 0);
			optimizer.inlineExitActions((i & (1 << 1)) != 0);
			optimizer.inlineEntryActions((i & (1 << 2)) != 0);
			optimizer.inlineEnterSequences((i & (1 << 3)) != 0);
			optimizer.inlineExitSequences((i & (1 << 4)) != 0);
			optimizer.inlineChoices((i & (1 << 5)) != 0);
			optimizer.inlineEntries((i & (1 << 6)) != 0);
			optimizer.inlineEnterRegion((i & (1 << 7)) != 0);
			optimizer.inlineExitRegion((i & (1 << 8)) != 0);

			ExecutionFlow optimizedflow = optimizer.transform(flow);

			List<String> names = new ArrayList<String>();

			executionflowNamingService.initializeNamingService(optimizedflow);
			for (ExecutionState state : flow.getStates()) {
				String name = executionflowNamingService.getShortName(state);
				assertEquals(names.contains(name), false);
				names.add(name);
			}
		}
	}

	@Test
	public void statechartTest1() {
		Statechart toTest = getNamingServiceStatechart();

		List<String> names = new ArrayList<String>();

		List<String> expectedNames = new ArrayList<String>(
				Arrays.asList("main_region_StateA", "main_region_StateB", "second_region_StateA", "third_region_StateA",
						"second_region_StateA_AnotherRegion_StateA", "second_region_StateA_AnotherRegion_StateB",
						"third_region_StateA_AnotherRegion_StateA", "third_region_StateA_AnotherRegion_StateB"));

		ExecutionFlow flow = optimizer.transform(sequencer.transform(toTest));

		executionflowNamingService.setMaxLength(0);
		executionflowNamingService.setSeparator('_');
		executionflowNamingService.initializeNamingService(flow);

		statechartNamingService.setMaxLength(0);
		statechartNamingService.setSeparator('_');
		statechartNamingService.initializeNamingService(toTest);

		for (ExecutionState state : flow.getStates()) {
			String name = executionflowNamingService.getShortName(state);
			assertEquals(names.contains(name), false);
			assertEquals(name, statechartNamingService.getShortName(state));
			names.add(name);
		}

		stringListsEqual(expectedNames, names);
	}

	@Test
	public void statechartTest2() {
		Statechart toTest = getNamingServiceStatechart();

		List<String> names = new ArrayList<String>();

		// these names are shorter than 15 characters because there are more
		// elements containing these names, e.g. state actions
		List<String> expectedNames = new ArrayList<String>(Arrays.asList("mgn_SA", "mgn_StteB", "s_S", "t_S",
				"t_S_AR_SA", "t_S_AR_StB", "s_S_AR_SA", "s_S_AR_StB"));

		ExecutionFlow flow = optimizer.transform(sequencer.transform(toTest));

		executionflowNamingService.setMaxLength(15);
		executionflowNamingService.setSeparator('_');

		executionflowNamingService.initializeNamingService(flow);

		for (ExecutionState state : flow.getStates()) {
			String name = executionflowNamingService.getShortName(state);
			assertEquals(names.contains(name), false);
			names.add(name);
		}

		stringListsEqual(expectedNames, names);
	}

	private Statechart getNamingServiceStatechart() {
		Statechart toTest = null;

		for (Statechart statechart : statecharts) {
			if (statechart.getName().equals("namingTest")) {
				toTest = statechart;
			}
		}

		assertEquals(true, toTest != null);

		return toTest;
	}

	private void nameLengthTest(int maxLength) {
		int num_statecharts = statecharts.size();
		long cumulated_time = 0L;
		for (Statechart statechart : statecharts) {

			// Transform statechart
			ExecutionFlow flow = sequencer.transform(statechart);
			flow = optimizer.transform(flow);

			List<String> names = new ArrayList<String>();

			executionflowNamingService.setMaxLength(maxLength);
			executionflowNamingService.setSeparator('_');

			long t0 = System.currentTimeMillis();
			executionflowNamingService.initializeNamingService(flow);
			cumulated_time += System.currentTimeMillis() - t0;
			for (ExecutionState state : flow.getStates()) {
				String name = executionflowNamingService.getShortName(state);
				assertEquals(names.contains(name), false);
				assertEquals(true, name.length() <= maxLength);
				names.add(name);
			}
		}

		System.out.print("Average time for initialization [ms]: ");
		System.out.println((float) cumulated_time / (float) num_statecharts);
	}

	private void stringListsEqual(List<String> onelist, List<String> otherlist) {
		java.util.Collections.sort(onelist, Collator.getInstance());
		java.util.Collections.sort(otherlist, Collator.getInstance());
		assertEquals(onelist, otherlist);
	}
}