package com.github.manikmagar.maven.versioner.core.version;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import static java.lang.String.valueOf;

public class VersionPatternStrategy extends SemVerStrategy {

	public static final String DEFAULT_VERSION_PATTERN = "%M.%m.%p(-%c)";
	private final String versionPattern;

	public VersionPatternStrategy(int major, int minor, int patch, String branchName, String hashRef,
			String versionPattern) {
		super(major, minor, patch, branchName, hashRef);
		if (versionPattern == null || versionPattern.trim().isEmpty()) {
			this.versionPattern = DEFAULT_VERSION_PATTERN;
		} else {
			this.versionPattern = versionPattern;
		}
	}

	public VersionPatternStrategy(String branchName, String hashRef, String versionPattern) {
		super(branchName, hashRef);
		this.versionPattern = versionPattern;
	}

	public VersionPatternStrategy(int major, int minor, int patch, String branchName, String hashRef) {
		this(major, minor, patch, branchName, hashRef, DEFAULT_VERSION_PATTERN);
	}

	public VersionPatternStrategy(String branchName, String hashRef) {
		this(branchName, hashRef, DEFAULT_VERSION_PATTERN);
	}

	public String getVersionPattern() {
		return versionPattern;
	}

	@Override
	public String toVersionString() {
		int patch = getVersion().getPatch();
		int commits = getVersion().getCommit();
		return new TokenReplacer(getVersionPattern())
				.replace(PatternToken.MAJOR, getVersion().getMajor())
				.replace(PatternToken.MINOR, getVersion().getMinor())
				.replace(PatternToken.PATCH, patch)
				.replace(PatternToken.SMART_PATCH, patch + (commits > 0 ? 1 : 0))
				.replace(PatternToken.COMMIT, commits)
				.replace(PatternToken.SILENT_COMMIT, commits)
				.replace(PatternToken.BRANCH, getVersion().getBranch())
				.replace(PatternToken.HASH_SHORT, getVersion().getHashShort())
				.replace(PatternToken.HASH, getVersion().getHash()).toString();
	}

	public static class TokenReplacer {
		private String text;

		public TokenReplacer(String text) {
			this.text = text;
		}

		public TokenReplacer replace(PatternToken token, int value) {
			return replace(token, valueOf(value));
		}

		public TokenReplacer replace(PatternToken token, String value) {
			if (!text.contains(token.getToken()))
				return this;
			Set<PatternToken> COMMIT_TOKENS = EnumSet.of(PatternToken.COMMIT, PatternToken.SILENT_COMMIT);
			if (!COMMIT_TOKENS.contains(token)) {
				if (text.contains(token.getToken())) {
					text = text.replace(token.getToken(), value);
				}
			} else {
				String repl = (token == PatternToken.SILENT_COMMIT) ? "" : value;
				// Full regex to match the version string containing group regex
				var fullRegex = ".*" + token.getTokenGroupRegex() + ".*";
				if (Pattern.matches(fullRegex, text)) {
					if (value != null && !value.trim().isEmpty() && !value.equals("0")) {
						text = text.replace(token.getToken(), repl);
					} else {
						text = text.replaceAll(token.getTokenGroupRegex(), "");
					}
				} else if (text.contains(token.getToken())) {
						text = text.replace(token.getToken(), repl);
					}
				}
			return this;
		}

		private String deTokenized() {
			return text.replace("(", "").replace(")", "");
		}

		@Override
		public String toString() {
			return deTokenized();
		}
	}

	public enum PatternToken {
		MAJOR("%M"), MINOR("%m"), PATCH("%p"), SMART_PATCH("%P"), COMMIT("%c"), SILENT_COMMIT("%C"),
		BRANCH("%b"), HASH_SHORT("%h"), HASH("%H");

		private final String token;
		private final String tokenGroupRegex;

		PatternToken(String token) {
			this.token = token;
			this.tokenGroupRegex = String.format("(\\([^(]*%s[^)]*\\))", token);
		}

		public String getToken() {
			return token;
		}

		public String getTokenGroupRegex() {
			return tokenGroupRegex;
		}

		@Override
		public String toString() {
			return getToken();
		}
	}

}
