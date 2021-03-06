package com.eco.bio7.scenebuilder.xmleditor;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.*;
import com.eco.bio7.scenebuilder.xmleditor.ColorManager;
import com.eco.bio7.scenebuilder.xmleditor.IXMLColorConstants;
import com.eco.bio7.scenebuilder.xmleditor.XMLWhitespaceDetector;
import com.eco.bio7.util.Util;

public class XMLTagScanner extends RuleBasedScanner {

	public XMLTagScanner(ColorManager manager) {
		IToken string;
		if (Util.isThemeBlack()) {
			 string = new Token(new TextAttribute(manager.getColor(IXMLColorConstants.STRING)));
		} else {
			 string = new Token(new TextAttribute(manager.getColor(IXMLColorConstants.STRING)));
		}

		IRule[] rules = new IRule[3];

		// Add rule for double quotes
		rules[0] = new SingleLineRule("\"", "\"", string, '\\');
		// Add a rule for single quotes
		rules[1] = new SingleLineRule("'", "'", string, '\\');
		// Add generic whitespace rule.
		rules[2] = new WhitespaceRule(new XMLWhitespaceDetector());

		setRules(rules);
	}
}
