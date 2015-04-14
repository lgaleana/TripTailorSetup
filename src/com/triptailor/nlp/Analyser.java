package com.triptailor.nlp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.triptailor.setup.TripTailorSetup;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

public class Analyser {
	
	final int BASE_YEAR = 2016;
	
	static StanfordCoreNLP pipeline;
	static {
		Properties props = new Properties();
	    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment");
	    pipeline = new StanfordCoreNLP(props);
	}
	
	private Map<String, double[]> vector;
	
	public Analyser() {
		this.vector = new HashMap<String, double[]>();
	}
	
	public void nlpAnalyse(String path, String hostelName) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(path));
		
		boolean isReview = false;
		int year = 2014;
		while(true) {
			char c;
			String line = "";
			do {
				c = (char) reader.read();
				line += c;
			} while(c != 10 && c != 65535);
			
			if(c != 65535) {
				if(isReview) {
					nlp(line, year, hostelName);
					System.out.print(".");
				}
				else
					year = getYear(line);
			}
			else
				break;
			isReview = !isReview;
		}
		
		reader.close();
	}
	
	private void nlp(String text, int year, String hostelName) {
	    Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    
		for (CoreMap sentence : sentences) {
			List<String> words = new ArrayList<String>();
			
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String pos = token.get(PartOfSpeechAnnotation.class);
				String word = token.get(LemmaAnnotation.class).toLowerCase();
				
				if ((pos.equals("NN") || pos.equals("NNS") || pos.equals("JJ")) && !TripTailorSetup.stop.containsKey(word) &&
						!word.matches(".*[^a-zA-Z-]+.*") && !words.contains(word))
					words.add(word);
			}

			Tree tree = sentence.get(SentimentCoreAnnotations.AnnotatedTree.class);
			int sentiment = RNNCoreAnnotations.getPredictedClass(tree) - 1;
			
			double timeModifier = 1 / Math.log(BASE_YEAR - year);
			
			for(String word : words) {
				double[] holder = vector.get(word);
				if(holder == null)
					holder = new double[3];
				
				holder[TripTailorSetup.FREQ]++;
				holder[TripTailorSetup.CFREQ] += timeModifier;
				holder[TripTailorSetup.RATING] += sentiment;
				
				vector.put(word, holder);
			}
		}
	}
	
	private int getYear(String line) {
		String[] info = line.split(",");
		if(info.length > 1) {
			String[] date = info[1].split(" ");
			if(date.length > 2)
				return Integer.parseInt(date[2]);
		}
		return 2000;
	}
	
	public Map<String, double[]> getVector() {
		return vector;
	}
}
