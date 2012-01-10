package com.github.searls.jasmine.coffee;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.lang.StringEscapeUtils;

import com.gargoylesoftware.htmlunit.MockWebConnection;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.github.searls.jasmine.io.IOUtilsWrapper;

public class CoffeeScript {

	private static Map<String,String> cache = Collections.synchronizedMap(new WeakHashMap<String,String>());
	
	private ThreadLocal<HtmlPage> htmlPage = new ThreadLocal<HtmlPage>() {
		@Override
		protected HtmlPage initialValue() {
			MockWebConnection webConnection = new MockWebConnection();
			WebClient webClient = new WebClient();
			webClient.setWebConnection(webConnection);
			try {
				HtmlPage page = webClient.getPage(WebClient.URL_ABOUT_BLANK);
				page.executeJavaScript(ioUtilsWrapper.toString("/vendor/js/coffee-script.js"));
				return page;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};
	
	private IOUtilsWrapper ioUtilsWrapper = new IOUtilsWrapper();

	public String compile(String coffee, boolean bareOption) throws IOException {
		CoffeeEval coffeeEval = new CoffeeEval(coffee, bareOption);
		String escapedCoffee = coffeeEval.getCacheKey();
		return cache.containsKey(escapedCoffee) ? cache.get(escapedCoffee) : compileAndCache(coffeeEval);
	}

	private String compileAndCache(CoffeeEval inputEval) {
		ScriptResult scriptResult = htmlPage.get().executeJavaScript(inputEval.createCoffeeScriptFunction());
		String result = (String) scriptResult.getJavaScriptResult();
		cache.put(inputEval.getCacheKey(),result);
		return result;
	}
	
	public static class CoffeeEval {
		private String coffee;
		private boolean bareOption;
		
		private String escapeCoffeeCache;
		
		private static final String BARE_OPTION_ENABLED = "{bare: true}";
		
		public CoffeeEval(String coffee, boolean bareOption) {
			this.coffee = coffee;
			this.bareOption = bareOption;
		}

		public String getCacheKey() {
			return String.format("Eval#%s,Options#%s", this.escape(), getOptions());
		}
		
		public String escape() {
			if (this.escapeCoffeeCache == null) {
				this.escapeCoffeeCache = StringEscapeUtils.escapeJavaScript(this.coffee);
			}
			return this.escapeCoffeeCache;
		}
		
		public String getOptions() {
			return this.bareOption ? BARE_OPTION_ENABLED : null;
		}
		
		public String createCoffeeScriptFunction() {
			StringBuilder function = new StringBuilder();
			function.append("CoffeeScript.compile(\"");
			function.append(escape());
			function.append("\"");
			if (bareOption) {
				function.append(", ");
				function.append(BARE_OPTION_ENABLED);
			}
			function.append(");");
			return function.toString();
		}
	}

}
