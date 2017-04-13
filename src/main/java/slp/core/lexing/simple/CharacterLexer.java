package slp.core.lexing.simple;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;

public class CharacterLexer implements Lexer {

	@Override
	public Stream<Stream<String>> lex(List<String> lines) {
		return lines.stream().map(line ->
				Arrays.stream(line.split("")));
	}
}
