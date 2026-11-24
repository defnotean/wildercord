package dev.wildercord.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves Wildercord's 9,400+ client-side translation keys ({@code en_us.json}) on the server for
 * Bedrock/Console clients so Geyser never displays raw {@code item.wildercord.*} or
 * {@code message.wildercord.*} keys in chat, actionbars, boss bars, titles, books, or tooltips.
 */
public final class BedrockTextTranslator {
	private static final String LANG_PATH = "/assets/wildercord/lang/en_us.json";
	private static final Pattern PLACEHOLDER = Pattern.compile("%(?:(\\d+)\\$)?([-#+ 0,(]*\\d*(?:\\.\\d+)?[a-zA-Z%])");
	private static volatile Map<String, String> translations;

	private BedrockTextTranslator() {}

	private static Map<String, String> ensureLoaded() {
		Map<String, String> cached = translations;
		if (cached != null) {
			return cached;
		}
		synchronized (BedrockTextTranslator.class) {
			if (translations == null) {
				translations = loadLang();
			}
			return translations;
		}
	}

	private static Map<String, String> loadLang() {
		try (InputStream stream = BedrockTextTranslator.class.getResourceAsStream(LANG_PATH)) {
			if (stream == null) {
				return Map.of();
			}
			JsonObject obj = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			Map<String, String> map = new HashMap<>(obj.size() * 2);
			for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
				if (e.getValue().isJsonPrimitive()) {
					map.put(e.getKey(), e.getValue().getAsString());
				}
			}
			return Collections.unmodifiableMap(map);
		} catch (Exception ignored) {
			return Map.of();
		}
	}

	public static int keyCount() {
		return ensureLoaded().size();
	}

	public static String lookup(String key) {
		return key == null ? null : ensureLoaded().get(key);
	}

	public static boolean containsWildercordKey(Component component) {
		if (component == null) {
			return false;
		}
		if (component.getContents() instanceof TranslatableContents tc) {
			if (lookup(tc.getKey()) != null) {
				return true;
			}
			for (Object arg : tc.getArgs()) {
				if (arg instanceof Component argComp && containsWildercordKey(argComp)) {
					return true;
				}
			}
		}
		for (Component sib : component.getSiblings()) {
			if (containsWildercordKey(sib)) {
				return true;
			}
		}
		return false;
	}

	public static Component translate(Component component) {
		if (component == null || !containsWildercordKey(component)) {
			return component;
		}
		return translateRecursive(component);
	}

	private static Component translateRecursive(Component component) {
		MutableComponent out;
		if (component.getContents() instanceof TranslatableContents tc) {
			String template = lookup(tc.getKey());
			Object[] args = tc.getArgs();
			if (template != null) {
				out = buildFormattedComponent(template, args).withStyle(component.getStyle());
			} else {
				Object[] mappedArgs = new Object[args.length];
				for (int i = 0; i < args.length; i++) {
					mappedArgs[i] = args[i] instanceof Component c ? translateRecursive(c) : args[i];
				}
				out = Component.translatableWithFallback(tc.getKey(), tc.getFallback(), mappedArgs)
					.withStyle(component.getStyle());
			}
		} else {
			out = component.plainCopy().withStyle(component.getStyle());
		}
		for (Component sib : component.getSiblings()) {
			out.append(translateRecursive(sib));
		}
		return out;
	}

	private static MutableComponent buildFormattedComponent(String template, Object... args) {
		MutableComponent result = Component.empty();
		Matcher m = PLACEHOLDER.matcher(template);
		int last = 0;
		int seq = 0;
		while (m.find()) {
			if (m.start() > last) {
				result.append(Component.literal(template.substring(last, m.start())));
			}
			String conv = m.group(2);
			if ("%".equals(conv)) {
				result.append(Component.literal("%"));
			} else {
				int idx = m.group(1) != null ? Integer.parseInt(m.group(1)) - 1 : seq++;
				if (args != null && idx >= 0 && idx < args.length) {
					Object arg = args[idx];
					if (arg instanceof Component comp) {
						result.append(translateRecursive(comp));
					} else {
						result.append(Component.literal(formatScalarArg(conv, arg)));
					}
				} else {
					result.append(Component.literal(m.group(0)));
				}
			}
			last = m.end();
		}
		if (last < template.length()) {
			result.append(Component.literal(template.substring(last)));
		}
		return result;
	}

	public static String formatTemplate(String template, Object... args) {
		if (template == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(template.length());
		Matcher m = PLACEHOLDER.matcher(template);
		int last = 0;
		int seq = 0;
		while (m.find()) {
			sb.append(template, last, m.start());
			String conv = m.group(2);
			if ("%".equals(conv)) {
				sb.append('%');
			} else {
				int idx = m.group(1) != null ? Integer.parseInt(m.group(1)) - 1 : seq++;
				if (args != null && idx >= 0 && idx < args.length) {
					Object arg = args[idx];
					sb.append(arg instanceof Component c ? translate(c).getString() : formatScalarArg(conv, arg));
				} else {
					sb.append(m.group(0));
				}
			}
			last = m.end();
		}
		sb.append(template, last, template.length());
		return sb.toString();
	}

	private static String formatScalarArg(String conv, Object arg) {
		if (arg == null) {
			return "null";
		}
		if ("s".equals(conv)) {
			return String.valueOf(arg);
		}
		try {
			return String.format(Locale.ROOT, "%" + conv, arg);
		} catch (Exception ignored) {
			return String.valueOf(arg);
		}
	}
}
