/*
 * Copyright 2023 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.example.bot.spring.echo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.ReplyMessageRequest;
import com.linecorp.bot.messaging.model.TextMessage;
import com.linecorp.bot.spring.boot.handler.annotation.EventMapping;
import com.linecorp.bot.spring.boot.handler.annotation.LineMessageHandler;
import com.linecorp.bot.webhook.model.Event;
import com.linecorp.bot.webhook.model.MessageEvent;
import com.linecorp.bot.webhook.model.TextMessageContent;

import java.io.*;
import java.util.*;

import javax.net.ssl.HttpsURLConnection;
import java.net.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

@SpringBootApplication
@LineMessageHandler
public class EchoApplication {
    private final Logger log = LoggerFactory.getLogger(EchoApplication.class);
    private final MessagingApiClient messagingApiClient;
    // Google Knowledge Graph Search APIの認証用API Keyに変える
    static String gkgsApiKey = "AIzaSyAlIM0_kJy4NG43v0wbw6avL5vLnkaWtg0";

    public static void main(String[] args) {
        SpringApplication.run(EchoApplication.class, args);
    }

    public EchoApplication(MessagingApiClient messagingApiClient) {
        this.messagingApiClient = messagingApiClient;
    }

    @EventMapping
    public void handleTextMessageEvent(MessageEvent event) {
        log.info("event: " + event);
        if (event.message() instanceof TextMessageContent) {
            TextMessageContent message = (TextMessageContent) event.message();

            String botResp = "";
            if (message.text().equals("ばいばい") || message.text().equals("exit")) {
                botResp = "ばいばい";
            } else {
                botResp = generateResponse(message.text()); // botの返答を生成
            }

            final String originalMessageText = botResp;
            messagingApiClient.replyMessage(new ReplyMessageRequest(
                    event.replyToken(),
                    List.of(new TextMessage(originalMessageText)),
                    false));
        }
    }

    @EventMapping
    public void handleDefaultMessageEvent(Event event) {
        System.out.println("event: " + event);
    }

    /**
     * botの返答を生成して返す
     *
     * @param userInput
     * @return 生成したbotの返答
     */
    public static String generateResponse(String userInput) {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize(userInput);
        String entity = "";
        String property = "";
        List<String> list1 = new ArrayList<>();
        List<String> list2 = new ArrayList<>();
        boolean isProperty = false;
        List<String> ans = new ArrayList<>(); // botの返答単語保存用
        for (Token token : tokens) {
            String part = token.getPartOfSpeechLevel1();
            String surface = token.getSurface();

            if (surface.equals("の"))
                isProperty = true;
            if (part.equals("名詞") && !isProperty) {
                entity += surface;
                list1.add(surface);
            } else if (part.equals("名詞") && isProperty) {
                property += surface;
                list2.add(surface);
            }
        }

        if (isProperty && list2.size() > 0) {
            // 質問応答bot
            System.out.println(entity);
            String wdJson = getWikidataJson(entity);
            Map<String, Object> wdMap = json2Map(wdJson);
            System.out.println(property);
            List<String> ids = getWikidataPropIds(property);
            System.out.println(ids);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resList = (List<Map<String, Object>>) wdMap.get("result"); // 検索結果のリスト
            for (Map<String, Object> res : resList) { // 検索結果resを1つずつ取り出す
                for (String prop : ids) {
                    String entityID = getEntityID(res); // resのエンティティIDを取得
                    List<String> propVals = getPropVals(res, prop); // プロパティ値のリストを取得
                    System.out.println("エンティティ " + entityID + "のプロパティ" + prop + ": " + propVals);
                    ans.addAll(propVals);
                }
                break;
            }
            if (ans.size() > 0) {
                String words = ans.get(0);
                for (int i = 1; i < ans.size(); i++) {
                    words += "、" + ans.get(i);
                }
                return entity + "の" + property + "は" + words + "ですよ。"+getWikipedia(entity,property);
            } 
        } 
        // 雑学ネタ提供bot
        entity = list1.get((int) Math.floor(Math.random() * list1.size()));
        String property2 = "";
        String wdJson = getWikidataJson(entity);
        Map<String, Object> wdMap = json2Map(wdJson);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resList = (List<Map<String, Object>>) wdMap.get("result"); // 検索結果のリスト
        for (Map<String, Object> res : resList) { // 検索結果resを1つずつ取り出す

            String entityID = getEntityID(res); // resのエンティティIDを取得
            String prop = getProp(res);
            System.out.println(prop);
            List<String> propVals = getPropVals(res, prop); // プロパティ値の日本語ラベルリストを取得
            System.out.println(getPropVals(res, prop));
            property2 = getLabelById(prop); // プロパティの日本語ラベルを取得
            System.out.println("エンティティ " + entityID + "のプロパティ" + prop + ": " + propVals);
            ans.addAll(propVals);

            break;
        } 
        if (ans.size() > 0) {
                String words = ans.get(0);
            for (int i = 1; i < ans.size(); i++) {
                words += "、" + ans.get(i);
            }
            String wiki1 = property.length()>0 ? getWikipedia(entity,property) : "わかりません。";
            return wiki1 + entity+"といえば、"+ entity+"の" +property2 +"は"+ words+ "ですよ。"+getWikipedia(entity,property2);
        } else
            return "「" + userInput + "」ですか、へぇー。"+getWikipedia(entity,property2);
        
    }

    /**
	 * wikipediaの情報を返す
	 *
	 * @param entity, property
	 * @return wikipediaの情報
	 * @throws IOException
	 */
	public static String getWikipedia(String entity, String property){
		// Wikipedia
		Document document = null;
		try {
			document = Jsoup.connect("https://ja.wikipedia.org/wiki/"+entity).get();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			return "";
		}
		Element mainContent = document.select("main#content").first();
		Elements paragraphs = mainContent.select("p, dd, li");
		String content = "";
		for (Element paragraph : paragraphs) {
			content += (paragraph.text());
			String[] sens = content.split("。");
			for (String sen : sens) {
				if (sen.contains(property)) {
					return sen+"。";
				}
			}
		}
		return "";
	}

    /**
     * resのエンティティIDを返す
     *
     * @param res
     * @return resのエンティティID
     */
    public static String getEntityID(Map<String, Object> res) {
        
        @SuppressWarnings("unchecked")
        String id = ((Map<String, Object>) res.get("entities")).keySet().iterator().next();
        return id;
    }

    /**
     * resに含まれているプロパティpropの値のリストを返す
     *
     * @param map
     * @param prop
     * @return プロパティ値のリスト
     */
    public static List<String> getPropVals(Map<String, Object> res, String prop) {
        List<String> vals = new ArrayList<String>();
        String entityID = getEntityID(res);
        @SuppressWarnings("unchecked")
        Map<String, Object> entityMap = (Map<String, Object>) ((Map<String, Object>) res.get("entities")).get(entityID);
        @SuppressWarnings("unchecked")
        Map<String, Object> claimMap = (Map<String, Object>) entityMap.get("claims");
        if (claimMap != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> propList = (List<Map<String, Object>>) claimMap.get(prop);
            if (propList != null) {
                for (Map<String, Object> propMap : propList) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> valMap = (Map<String, Object>) ((Map<String, Object>) propMap.get("mainsnak")).get("datavalue");
                    Object val = valMap.get("value");
                    if (val instanceof String) { // valがString型なら
                        vals.add((String) val);
                    } else if (val instanceof Map<?, ?>) { // valがMap<String, Object>型なら
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) val;
                        String id = (String) map.get("id");
                        String am = (String) map.get("amount");
                        if (id != null) {
                            String jaLabel = getLabelById(id); // getLabelByIdメソッドは自分で実装
                            vals.add(jaLabel);
                        }
                        if (am != null) {
                            vals.add((String) map.get("amount"));
                        }
                    } else { // それ以外なら
                        vals.add(val.toString());
                    }
                }
            }
        }
        return vals;
    }

    public static String getProp(Map<String, Object> res) {
        List<String> vals = new ArrayList<String>();
        String entityID = getEntityID(res);
        @SuppressWarnings("unchecked")
        Map<String, Object> entityMap = (Map<String, Object>) ((Map<String, Object>) res.get("entities")).get(entityID);
        @SuppressWarnings("unchecked")
        Map<String, Object> claimMap = (Map<String, Object>) entityMap.get("claims");
        boolean isMap = false;
        if (claimMap != null) {
            List<String> list = new ArrayList<String>(claimMap.keySet());
            String prop = "";
            while (!isMap) { // val == nullをはじく
                int index = new Random().nextInt(list.size());
                prop = list.get(index);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> propList = (List<Map<String, Object>>) claimMap.get(prop);
                if (propList != null) {
                    for (Map<String, Object> propMap : propList) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> valMap = (Map<String, Object>) ((Map<String, Object>) propMap.get("mainsnak")).get("datavalue");
                        Object val = valMap.get("value");
                        System.out.println(val);
                        if (val != null)
                            return prop;
                    }
                }
            }

        }
        return "";

    }

    private static String getLabelById(String id) {
        String wdJson = getWikidataJson(id);
        Map<String, Object> wdMap = json2Map(wdJson);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) wdMap.get("result");
        Map<String, Object> resultMap = resultList.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> entityMap = (Map<String, Object>) ((Map<String, Object>) resultMap.get("entities")).get(id);
        @SuppressWarnings("unchecked")
        Map<String, Object> labelMap = (Map<String, Object>) entityMap.get("labels");
        if (labelMap.containsKey("ja")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> jaMap = (Map<String, Object>) labelMap.get("ja");
            Object val = (jaMap.get("value"));
            return (String) val;
        } else {
            @SuppressWarnings("unchecked")
            Map<String, Object> enMap = (Map<String, Object>) labelMap.get("en");
            Object val = (enMap.get("value"));
            return (String) val;
        }
    }

    /**
     * Wikidataからデータを検索
     *
     * @param query
     * @return Wikidataから取得したJSON文字列
     */
    public static String getWikidataJson(String query) {
        StringBuffer sb = new StringBuffer();
        sb.append("{\"result\":[");
        int initLen = sb.length();
        List<String> ids;
        if (query.startsWith("P"))
            ids = getWikidataPropIds(query);
        else
            ids = getWikidataIds(query);
        for (String id : ids) {
            if (sb.length() > initLen) {
                sb.append(",");
            }
            String url = "https://www.wikidata.org/wiki/Special:EntityData/" + id + ".json";
            String json = getData(url);
            sb.append(getData(url));
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * WikidataエンティティのIDを検索
     *
     * @param query
     * @return WikidataエンティティのIDのリスト
     */
    public static List<String> getWikidataIds(String query) {
        String encodedQuery = "";
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = "https://www.wikidata.org/w/api.php?action=wbsearchentities&language=ja&format=json&search="
                + encodedQuery;
        Map<String, Object> map = json2Map(getData(url));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("search");
        List<String> ids = new ArrayList<String>();
        for (Map<String, Object> entMap : list) {
            String id = (String) entMap.get("id");
            ids.add(id);
        }
        return ids;
    }

    /**
     * WikidataプロパティのIDを検索
     *
     * @param query
     * @return WikidataプロパティのIDのリスト
     */
    public static List<String> getWikidataPropIds(String query) {
        String encodedQuery = "";
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = "https://www.wikidata.org/w/api.php?action=wbsearchentities&language=ja&format=json&type=property&search="
                + encodedQuery;

        Map<String, Object> map = json2Map(getData(url));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("search");
        List<String> ids = new ArrayList<String>();
        for (Map<String, Object> entMap : list) {
            String id = (String) entMap.get("id");
            ids.add(id);
        }
        return ids;
    }

    /**
     * Google Knowledge Graph からデータを検索
     *
     * @param query
     * @return Google Knowledge Graphから取得したJSON文字列
     */
    public static String getGoogleKGJson(String query) {
        String encodedQuery = "";
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = "https://kgsearch.googleapis.com/v1/entities:search?query=" + encodedQuery + "&languages=ja&key="
                + gkgsApiKey;
        return getData(url);
    }

    /**
     * JSON形式の文字列をMapに変換
     *
     * @param json
     * @return JSONから変換したMapオブジェクト
     */
    public static Map<String, Object> json2Map(String json) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = new HashMap<String, Object>();
        map = null;
        try {
            map = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * HTMLに限らない形式のデータをWebから取得
     *
     * @param url
     * @return 返ってきたデータ
     */
    public static String getData(String url) {
        String enc = "UTF-8";
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader in = null;
            if (url.startsWith("https")) {
                HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
                in = new BufferedReader(new InputStreamReader(conn.getInputStream(), enc));
            } else {
                URLConnection conn = new URL(url).openConnection();
                in = new BufferedReader(new InputStreamReader(conn.getInputStream(), enc));
            }
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
