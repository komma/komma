/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.generator.java.merge;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.emf.codegen.merge.java.JControlModel;
import org.eclipse.emf.codegen.merge.java.JMerger;
import org.eclipse.emf.codegen.merge.java.facade.ast.ASTFacadeHelper;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives the java output of a generate action and the target location. Reads
 * the current source from there and merges the generation output and the
 * current content.
 * 
 */
public class SourceMerger {
	private final Logger log = LoggerFactory.getLogger(SourceMerger.class);

	private JControlModel jControlModel;
	private CodeFormatter codeFormatter;

	public String forType() {
		return "java";
	}

	/**
	 * Does the merge operation and returns the new content if the content has
	 * really changed, otherwise null is returned.
	 */
	public String merge(File targetFile, String content) {
		try {
			final String newSource;
			if (targetFile.exists()) {
				log.debug("Current source exists, use JMerge");
				final JControlModel jControlModel = getJControlModel();
				final JMerger jMerger = new JMerger(jControlModel);
				jMerger.setFixInterfaceBrace(jControlModel.getFacadeHelper()
						.fixInterfaceBrace());
				try {
					jMerger.setSourceCompilationUnit(jMerger
							.createCompilationUnitForContents(content));
				} catch (Exception e) { // something wrong in the code itself
					throw new Exception("Syntax error in generated source for "
							+ targetFile.getName() + " :"
							+ getExceptionMessage(e)
							+ "\nSource>>>>>>>>>>>>>>>>>>>>>>>>>\n" + content,
							e);
				}
				try {
					jMerger.setTargetCompilationUnit(jMerger
							.createCompilationUnitForInputStream(new FileInputStream(
									targetFile)));
				} catch (Exception e) { // something wrong in the code itself
					throw new Exception("Syntax error in current source for "
							+ targetFile.getName() + " :"
							+ getExceptionMessage(e), e);
				} catch (Throwable t) {
					throw new Exception("Syntax error in current source for "
							+ targetFile.getName() + " :"
							+ getExceptionMessage(t), t);
				}

				final String oldSource = jMerger
						.getTargetCompilationUnitContents();
				jMerger.merge();

				String mergedContent = jMerger
						.getTargetCompilationUnitContents();

				System.err.println("old: \n" + oldSource);
				System.err.println("new: \n" + mergedContent);

				TextEdit edit = getCodeFormatter().format(
						CodeFormatter.K_COMPILATION_UNIT, mergedContent, 0,
						mergedContent.length(), 0, "\n");
				newSource = edit.toString();

				// TODO: check if target is read only!
				jControlModel.getFacadeHelper().reset();

				if (newSource.equals(oldSource)) {
					return null; // nothing changed
				}
				return newSource;
			} else {
				log.debug("Current source does not exist, create one");
				TextEdit edit = getCodeFormatter().format(
						CodeFormatter.K_COMPILATION_UNIT, content, 0,
						content.length(), 0, "\n");
				return edit.toString();
			}
		} catch (Exception e) {
			// catch them all
			throw new RuntimeException(
					"Exception while merging and saving source file "
							+ targetFile.getName(), e);
		}
	}

	/** Returns the local path for the type of content */
	public String getLocalPath(IProject project) {
		return "/src";
	}

	/** Determine the compile errors */
	private String getExceptionMessage(Throwable t) {
		// if (t.getCause() instanceof DiagnosticException) {
		// final DiagnosticException d = (DiagnosticException) t.getCause();
		// final StringBuilder message = new StringBuilder(d.getDiagnostic()
		// .getMessage());
		// for (Diagnostic cd : d.getDiagnostic().getChildren()) {
		// message.append("\n\t").append(cd.getMessage());
		// }
		// return message.toString();
		// } else {
		return t.getMessage();
		// }
	}

	/** @return the jMerge control model */
	private JControlModel getJControlModel() {
		if (jControlModel == null) {
			jControlModel = new JControlModel();
			jControlModel.initialize(new ASTFacadeHelper(), this.getClass()
					.getResource("merge.xml").toExternalForm());
		}
		return jControlModel;
	}

	private CodeFormatter getCodeFormatter() {
		if (codeFormatter == null) {
			@SuppressWarnings("unchecked")
			final Map<Object, Object> options = DefaultCodeFormatterConstants
					.getEclipseDefaultSettings();
			options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
			options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
					JavaCore.VERSION_1_5);
			options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);

			codeFormatter = ToolFactory.createCodeFormatter(options);
		}
		return codeFormatter;
	}
}
