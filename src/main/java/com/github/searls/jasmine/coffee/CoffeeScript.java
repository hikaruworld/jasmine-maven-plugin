package com.github.searls.jasmine.coffee;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;


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
		CoffeeCup coffeeEval = new CoffeeCup(coffee, bareOption);
		String escapedCoffee = coffeeEval.getCacheKey();
		return cache.containsKey(escapedCoffee) ? cache.get(escapedCoffee) : compileAndCache(coffeeEval);
	}

	private String compileAndCache(CoffeeCup inputEval) {
		ScriptResult scriptResult = htmlPage.get().executeJavaScript(inputEval.createCoffeeScriptFunction());
		String result = (String) scriptResult.getJavaScriptResult();
		cache.put(inputEval.getCacheKey(),result);
		return result;
	}

}
