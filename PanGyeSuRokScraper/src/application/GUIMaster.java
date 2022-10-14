package application;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class GUIMaster implements Initializable {

	@FXML
	private WebView wvMain;

	@FXML
	private Button btnStart;

	private WebEngine weMain;
	private ChangeListener<Worker.State> listener = (obs, oldState, newState) -> {
		if (newState == State.SUCCEEDED) {
			// new page has loaded, process:
			start();
		}
	};

	private int intCounter;

	private Random rand = new Random();

	private static final int INDENTATION_DIVIDOR = 20;
	private static final int APPENDIX_SCROLL_NUMBER = 27;
	private static final int OTHER_SCROLL_NUMBER = 28;
	private static final int SECONDS_IN_MILISECONDS = 1000;
	private static final int RANGE_MILISECONDS_TO_WAIT = 7;
	private static final int BASE_SECONDS = 4;
	private static final int LOAD_ERROR_THRESHHOLD_MILISECONDS = 60 * SECONDS_IN_MILISECONDS;

	private Timer loadErrorSolver;

	private String strURL;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		weMain = wvMain.getEngine();
		weMain.load("http://www.davincimap.co.kr/davBase/Source/davSource.jsp?"
				+ "Job=Body&SourID=SOUR001386&Lang=%ED%95%9C%EB%AC%B8&Page=27");
		intCounter = 27;
		loadErrorSolver = new Timer();
	}

	public void start() {
		// Get Table
		if (weMain.getDocument() == null) {
			System.out.println("Please be patient.");
			return;
		}
		loadErrorSolver.cancel();
		weMain.getLoadWorker().stateProperty().removeListener(listener);
		if (intCounter > OTHER_SCROLL_NUMBER) {
			return;
		}
		NodeList nlH3 = weMain.getDocument().getElementsByTagName("H3");
		Node nTable = nlH3.item(nlH3.getLength() - 1);
		Element eTable;
		while (nTable != null) {
			if (nTable.getNodeType() == Node.ELEMENT_NODE) {
				eTable = (Element) nTable;
				eTable.getNodeName();
				if ("TBODY".equals(eTable.getTagName())) {
					break;
				}
			}
			nTable = nTable.getParentNode();
		}

		// Scrape Page
		NodeList nlTR = nTable.getChildNodes();
		int intNewChars = ((Element) nTable).getElementsByTagName("IMG").getLength();
		String strScroll = "";
		for (int i = 0; i < nlTR.getLength(); i++) {
			if (nlTR.item(i).getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			NodeList nlTD = ((Element) nlTR.item(i)).getElementsByTagName("TD");
			if (nlTD == null || nlTD.getLength() == 0) {
				continue;
			}
			NodeList nlTextContent = nlTD.item(nlTD.getLength() - 1).getChildNodes();
			if (nlTextContent == null || nlTextContent.getLength() == 0) {
				continue;
			}
			Node nText = nlTextContent.item(nlTextContent.getLength() - 2);
			if (nText == null || nText.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			strScroll += paragraphParser((Element) nText);
		}

		strScroll = strScroll.trim();

		if (intNewChars != 0) {
			strScroll += "newchar" + intNewChars + strScroll;
		}

		// Create Page Header
		String strCurrentScroll = null;
		String strPreviousScroll = null;
		String strNextScroll = null;

		switch (intCounter) {
		case APPENDIX_SCROLL_NUMBER:
			strCurrentScroll = "附錄";
			break;
		case OTHER_SCROLL_NUMBER:
			strCurrentScroll = "遺卷";
			break;
		default:
			strCurrentScroll = "卷" + strIntToChinese("" + intCounter);
			break;
		}

		switch (intCounter - 1) {
		case APPENDIX_SCROLL_NUMBER:
			strPreviousScroll = "附錄";
			break;
		case OTHER_SCROLL_NUMBER:
			strPreviousScroll = "遺卷";
			break;
		default:
			if (intCounter - 1 <= 0) {
				break;
			}
			strPreviousScroll = "卷" + strIntToChinese("" + (intCounter - 1));
			break;
		}

		switch (intCounter + 1) {
		case APPENDIX_SCROLL_NUMBER:
			strNextScroll = "附錄";
			break;
		case OTHER_SCROLL_NUMBER:
			strNextScroll = "遺卷";
			break;
		default:
			if (intCounter + 1 > APPENDIX_SCROLL_NUMBER) {
				break;
			}
			strNextScroll = "卷" + strIntToChinese("" + (intCounter + 1));
			break;
		}

		String strHeader = "{{Header|title=" + strCurrentScroll + "|author=|section=|times=|y=|m=|d=";
		if (strPreviousScroll != null) {
			strHeader += "|previous=[[../" + strPreviousScroll + "|" + strPreviousScroll + "]]";
		}

		if (strNextScroll != null) {
			strHeader += "|next=[[../" + strNextScroll + "|" + strNextScroll + "]]";
		}
		strHeader += "}}\n";

		strScroll = strHeader + strScroll;
		// Store into text files
		try {
			InputStream input = new ByteArrayInputStream(strScroll.getBytes("UTF-8"));
			int length = 0;
			FileOutputStream fp = new FileOutputStream(
					"C:\\Users\\he_xi\\eclipse-workspace\\PanGyeSuRokScraper\\file\\" + intCounter++ + ".txt");
			byte[] buffer = new byte[2048];
			while ((length = input.read(buffer)) != -1) {
				fp.write(buffer, 0, length);
			}
			fp.close();
			input.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Go to next page
		NodeList nlA = weMain.getDocument().getElementsByTagName("A");
		weMain.getLoadWorker().stateProperty().addListener(listener);
		for (int i = 0; i < nlA.getLength(); i++) {
			if (nlA.item(i) != null && nlA.item(i).getNodeType() == Node.ELEMENT_NODE
					&& nlA.item(i).getTextContent().contains("다음")) {
				Element eA = (Element) nlA.item(i);
				try {
					Thread.sleep((BASE_SECONDS + rand.nextInt(RANGE_MILISECONDS_TO_WAIT)) * SECONDS_IN_MILISECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				strURL = "http://www.davincimap.co.kr/davBase/Source/" + eA.getAttribute("href");
				weMain.load(strURL);
				break;
			}
		}
		loadErrorSolver = new Timer(false);
		loadErrorSolver.schedule(new TimerTask() {
			@Override
			public void run() {
				Platform.runLater(() -> {
					weMain.load(strURL);
				});
			}
		}, LOAD_ERROR_THRESHHOLD_MILISECONDS, LOAD_ERROR_THRESHHOLD_MILISECONDS);
	}

	private String paragraphParser(Element eElement) {
		String strReturn = "";
		if ("DIV".equals(eElement.getTagName())) {
			String strIndentation = eElement.getAttribute("style");
			try {
				strIndentation = strIndentation.substring(strIndentation.lastIndexOf(":") + 1,
						strIndentation.lastIndexOf("p"));
			} catch (Exception e) {
				strIndentation = "0";
			}
			int intIndentation = Integer.parseInt(strIndentation) / INDENTATION_DIVIDOR - 1;
			NodeList nlChildren = eElement.getChildNodes();
			for (int i = 0; i < nlChildren.getLength(); i++) {
				if (nlChildren.item(i).getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element eChild = (Element) nlChildren.item(i);
				String strAttributes = eChild.getAttribute("style");
				if (strAttributes.contains("font-weight:normal")) {
					strReturn += "{{*|" + eChild.getTextContent() + "}}";
				} else {
					strReturn += eChild.getTextContent();
				}
			}
			if (intIndentation != 0) {
				strReturn = "{{dent|" + intIndentation + "cm||" + strReturn + "}}";
			}
			strReturn += "\n\n";
		} else {
			strReturn = eElement.getTextContent().trim();
			String strHeader = "";
			int intPeriodIndex = strReturn.indexOf('.');
			int intPeriods = 0;
			while (intPeriodIndex != -1) {
				intPeriods++;
				intPeriodIndex = strReturn.indexOf('.', intPeriodIndex + 1);
			}
			for (int i = 0; i < intPeriods; i++) {
				strHeader += "=";
			}
			strReturn = strHeader + strReturn.substring(strReturn.lastIndexOf('.') + 1).trim() + strHeader + "\n";
		}
		return strReturn.replace(" ", "");
	}

	private String strIntToChinese(String strString) {
		String strIntToReplace = "";
		int intIntTemp;
		int intIntToReplace;

		for (Character chrElement : strString.toCharArray()) {
			if (Character.isDigit(chrElement)) {
				strIntToReplace += chrElement;
			}
		}

		intIntToReplace = Integer.parseInt(strIntToReplace);
		strIntToReplace = "";
		intIntTemp = intIntToReplace;
		switch (intIntTemp / 10) {
		case 2:
			strIntToReplace += '二';
			break;
		case 3:
			strIntToReplace += '三';
			break;
		case 4:
			strIntToReplace += '四';
			break;
		case 5:
			strIntToReplace += '五';
			break;
		case 6:
			strIntToReplace += '六';
			break;
		case 7:
			strIntToReplace += '七';
			break;
		case 8:
			strIntToReplace += '八';
			break;
		case 9:
			strIntToReplace += '九';
			break;
		}
		if (intIntTemp / 10 != 0) {
			intIntTemp %= 10;
			strIntToReplace += '十';
		}
		switch (intIntTemp) {
		case 1:
			strIntToReplace += '一';
			break;
		case 2:
			strIntToReplace += '二';
			break;
		case 3:
			strIntToReplace += '三';
			break;
		case 4:
			strIntToReplace += '四';
			break;
		case 5:
			strIntToReplace += '五';
			break;
		case 6:
			strIntToReplace += '六';
			break;
		case 7:
			strIntToReplace += '七';
			break;
		case 8:
			strIntToReplace += '八';
			break;
		case 9:
			strIntToReplace += '九';
			break;
		}
		return strString.replace("" + intIntToReplace, strIntToReplace);
	}
}
