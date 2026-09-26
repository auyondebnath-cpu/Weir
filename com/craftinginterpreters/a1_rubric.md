# A1 Rubric Explanation

This document is part of your first submission.  Complete it and include it in your submission zip, alongside your parser, example programs.

## How it works

The rubric explanation is the set of questions below.  The first set, the basic questions, is graded directly and is worth 10\% of your marks for this submission.  Answer them accurately to earn those marks.

The remaining sections ask one question for each of the other rubric items.  These are not graded directly, but your answers help your marker award you the marks for each rubric item, so write your answers below each question text in markdown format and point your marker to where the evidence lives in your submission.

## Basic questions (10)

1. Which chapter of the book did you use as the starting point for your solution?

### Your answer

Chapter 6, "Parsing Expressions", of Crafting Interpreters. My Scanner, Token, TokenType, and Parser classes follow the recursive-descent structure introduced in Chapters 4-6, and my Expr/Stmt/GenerateAST classes follow the metaprogramming approach introduced in Chapter 5, "Representing Code".



2. What is the "working folder", and what command(s) compile your parser?

### Your answer

The working folder is the project root (the folder containing the `com` directory), e.g. `Weir\`. All source files live under `com\craftinginterpreters\weir\` (the language implementation) and `com\craftinginterpreters\tools\` (the `GenerateAST` metaprogramming tool).

To compile the parser, run from the working folder:

**javac -d out com\craftinginterpreters\weir\*.java**

This compiles all `.java` files in the `weir` package and places the resulting `.class` files in `out\com\craftinginterpreters\weir\`.

*To run the REPL:*

**java -cp out com.craftinginterpreters.weir.Weir**

To run a `.weir` program file:

**java -cp out com.craftinginterpreters.weir.Weir com\craftinginterpreters\programs\canberra.weir**

If you need to regenerate `Expr.java`/`Stmt.java` after changing the grammar description in `GenerateAST.java`, compile and run that tool first:

**javac -d out com\craftinginterpreters\tools\*.java**
**java -cp out com.craftinginterpreters.tools.GenerateAST com/craftinginterpreters/weir**




3. What literal in your language represents a river that gets 10L/s of flow on the first day after 1mm of rainfall?

### Your answer

root myriver f: 1 s: 1 m: 10;


This is Weir's flow literal, attached to a root river declaration. It has three labelled, fixed-order fields:

*f: — the first day water starts arriving (here, day 1).*

*s: — the spread, how drawn-out the flow is over time (here, minimal spread of 1).*

*m: — the magnitude, the total flow volume (here, 10, representing 10L/s).*

Each label is the first letter of its parameter name... See **com\craftinginterpreters\programs\canberra.weir** for this syntax used in a full working example.


4. What symbol in your language shows two rivers combine, and is it a "unary", "binary", or "literal"?

### Your answer

The `+` operator, used inside a `combination` (e.g. `river centralmolongolo = queanbeyan + uppermolongolo + jerrabombarra;`). It is a **binary** operator — each `+` combines exactly two river operands into a single `Expr.Binary` node. When more than two rivers combine, my parser chains them left-associatively, e.g. `queanbeyan + uppermolongolo + jerrabombarra` parses as `(queanbeyan + uppermolongolo) + jerrabombarra`, confirmed directly by my `AstPrinter`'s output when run on the Canberra example program.


5. Does your language include statements, or is it an expression language?

### Your answer

My language includes statements. `Stmt.java` defines seven statement types: `Root` (root river declaration with an optional flow literal), `RiverDecl` (derived river declaration), `Dam` (dam declaration containing rules), `DamRule` (a single `when`/`default` rule), `Connect` (`X flows Y;`), `Print`, and `Expression` (an expression used as a statement). Expressions (`Expr.java`: `Binary`, `Grouping`, `Literal`, `Unary`, `Variable`, `Flows`) are nested inside these statements rather than the language being purely expression-based. My `Parser.declaration()` method is the top-level entry point that dispatches to river declarations, dam declarations, or general statements.


6. In your language, how long does it take all the water to work through a river system after 1 day of rain?

### Your answer

10 days. This matches the assignment brief's stated simplifying assumption that "all water that falls from the sky will (within 10 days) make its way into the river." My root river declarations already capture the three inputs this propagation will eventually need, f: (start day), s: (spread), and m: (magnitude), but per the brief's own guidance that Submission One should "ignore the complication of representing the rainfall and focus on a language which is capable of describing the river system itself," these three fields are currently parsed and stored as plain values with no day-by-day arithmetic behind them yet.

The precise semantics I plan to implement in Submission Two: s is the number of consecutive days the flow spreads evenly across, starting at day f. Each of those s days receives an equal share, m / s, of the total magnitude, so f: 2 s: 4 m: 20 would mean days 2, 3, 4, and 5 each receive 5 units, and every day outside that window receives zero from this root river. This formula is fully computable (no ambiguity about what happens on any given day), stays within the brief's 10-day propagation assumption by construction once s is validated to not exceed 10, and is a deliberate design choice distinct from the exemplar's own stated approach of geometric decay from a single start day.

This is not yet implemented. Parser.java's flowField() currently only validates that f, s, and m are non-negative and that s does not exceed 10; it stores all three as raw Token values on Stmt.Root without computing any distribution, and AstPrinter.java simply prints those stored values back out unchanged. The m / s per-day split described above is planned Submission Two evaluation logic, not something reflected in the current parser or AST printer.

## Log-book submissions (10)

Which file in the zip are your log-book entries and when did you make them?  Your teacher needs to have seen them during the semester.

### Your answer

It is named as logbook.docx in the zipfile with inside com\craftinginterpreters alongside folders tools, weir and out. I made the entries regularly each week but I missed out some practical classes. However, I covered the topics in the next class and completed the entries.



## Grammar given in the document in Nystrom's notation (20)

Provide the grammar for your language, and how does each of your example programs parse according to it?

### Your answer

Here is my full grammar in Nystrom's notation, taken directly from the structure implemented in `Parser.java`:


program        -> declaration* EOF ;

declaration    -> riverDecl
                 | damDecl
                 | statement ;

riverDecl      -> "root" IDENTIFIER ( "f" ":" NUMBER "s" ":" NUMBER "m" ":" NUMBER )? ";"
                 | "river" IDENTIFIER "=" flowExpr ";" ;

flowExpr       -> combination ( "flows" IDENTIFIER )? ;

combination    -> IDENTIFIER ( "+" IDENTIFIER )* ;

damDecl        -> "dam" IDENTIFIER "{" damRule* defaultRule "}" ;

damRule        -> "when" condition ":" expression ";" ;

defaultRule    -> "default" ":" expression ";" ;

condition      -> "level" ( ">" | ">=" | "<" | "<=" | "==" | "!=" ) NUMBER ;

statement      -> connectStmt
                 | printStmt
                 | exprStmt ;

connectStmt    -> IDENTIFIER "flows" IDENTIFIER ";" ;

printStmt      -> "print" expression ";" ;

exprStmt       -> expression ";" ;

expression     -> equality ;
equality       -> comparison ( ( "!=" | "==" ) comparison )* ;
comparison     -> term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           -> factor ( ( "-" | "+" ) factor )* ;
factor         -> unary ( ( "/" | "*" ) unary )* ;
unary          -> ( "!" | "-" ) unary
                 | primary ;
primary        -> NUMBER | STRING | "true" | "false" | "nil"
                 | "inflow" | "level"
                 | IDENTIFIER
                 | "(" expression ")" ;

`canberra.weir` opens with `root` declarations for the three source rivers (`googong`, `jerrabombarra`, `uppermolongolo`), each matching `riverDecl -> "root" IDENTIFIER ( "f" ":" NUMBER ... )?`. `dam1` and `dam2` are parsed by `damDecl`, each containing one or more `damRule`s (matching `"when" condition ":" expression ";"`) followed by exactly one `defaultRule`. The `condition` in each rule matches `"level" comparisonOp NUMBER`, satisfying the `condition` production. Statements such as `googong flows dam1;` match `connectStmt -> IDENTIFIER "flows" IDENTIFIER ";"`. The combination line `river centralmolongolo = queanbeyan + uppermolongolo + jerrabombarra;` matches `riverDecl`'s second alternative, where the right-hand side is parsed by `flowExpr -> combination`, and `combination` recurses through repeated `"+" IDENTIFIER` to build a left-associative `Expr.Binary` chain. The final `print centralmolongolo;` (or equivalent output line) matches `printStmt`.

`brisbane.weir` follows the identical shape at larger scale: six `root` declarations, two `damDecl`s each with multiple `when` tiers (`somersetdam` has two, `wivenhoedam` has three, all matching `damRule*` before the mandatory `defaultRule`), several `connectStmt`s (`flows`), and two `river = combination` declarations, the second of which (`lowerbrisbane`) combines three identifiers and again demonstrates `combination`'s repetition.

`sluicegate.weir` is the deepest test of `damDecl` and `condition`: three dams (`noflowdam`, `halfflowdam`, `preciselevel`) are chained via `connectStmt`s, and between them they exercise every comparison operator in the `condition` production (`>=`, `>`, `<`, `==`, `!=`), each parsed as an `Expr.Binary` with a `Token` operator drawn from the corresponding `TokenType`. All three dams correctly terminate with a `defaultRule`, satisfying the grammar's requirement that `damDecl` always ends in exactly one `default` clause.

## Three example programs (20)

Provide your three example programs here and identify which files in your zip contain them.

### Your answer

I have included three example programs in the submission zip, located in com/craftinginterpreters/programs.

I have also added a description for each of the program:

1. **`canberra.weir`** — models the real Canberra river system described in the assignment brief (Figure 1): root rivers `googong`, `jerrabombarra`, and `uppermolongolo`; `googong` drains through `dam1` into `queanbeyan`; `centralmolongolo` combines `queanbeyan`, `uppermolongolo`, and `jerrabombarra`; `centralmolongolo` drains through `dam2` into `lowermolongolo`, the system's output. This program demonstrates the minimum required feature set: root rivers, one combination, two dams in sequence, and a final printed output.

2. **`brisbane.weir`** — models a simplified version of the real Brisbane River catchment in Queensland, sourced from public descriptions of the system. It extends beyond the Canberra example by using six root rivers instead of three, two dams in sequence (`somersetdam`, matching the real Somerset Dam upstream of Wivenhoe Dam on the Stanley River) each with a different number of `when` tiers, and two separate combination steps (a four-way combination feeding `wivenhoedam`, and a three-way combination producing the final output `lowerbrisbane`). This program demonstrates that my grammar scales to larger, more structurally varied river systems.

3. **`sluicegate.weir`** — a purpose-built program (not tied to a real place) designed specifically to exhaustively demonstrate dam syntax. It chains three dams in sequence: `noflowdam` demonstrates a hard cutoff using `>=` and a literal zero-multiplier release; `halfflowdam` demonstrates three graduated `when` tiers stepping the release fraction down as level rises, directly matching the brief's suggested "no-flow, half-flow, full-flow" dam behaviours; `preciselevel` exercises the remaining comparison operators (`<`, `==`, `!=`) that the other two programs don't use. This program exists to give explicit, isolated evidence that every comparison operator and every dam-rule pattern in my grammar is implemented and parses correctly.

All three files were run through my compiled parser and AST printer with no parse errors, confirming they are accepted by the grammar above.

## Parser written in Java based on Lox codebase (20)

Which chapter of the book is your parser based on?  What did you add beyond the Chapter 6 code, and where is that explained?

### Your answer

My parser is based on the Lox parser from Chapter 6 of *Crafting Interpreters* (the Parsing Expressions chapter), extended with the statement-handling structure introduced later in the book, adapted to the river-system domain rather than general-purpose Lox syntax.

Beyond the Chapter 6 baseline, I added:

- **New token types** in `TokenType.java` for the domain vocabulary: `RIVER`, `ROOT`, `DAM`, `FLOWS`, `WHEN`, `INFLOW`, `LEVEL`, `DEFAULT`, `RAINFALL`, `COLON`, and the flow-literal field labels `F`, `S`, `M`.

- **Scanner keyword support** in `Scanner.java` for all of the above, added to the existing keyword map alongside Lox's original keywords.

- **Note on `RAINFALL`:** This token is scaffolded now as a reserved keyword for Submission Two, where rainfall becomes a user-definable input to the interpreter (per the assignment brief's evaluation requirements). It is recognized by the Scanner but not yet consumed by any grammar production in this submission, since rainfall has no role in parsing — only in evaluation, which is out of scope for Submission One.

- **New expression node** `Expr.Flows` in `Expr.java` (generated via `GenerateAST.java`), representing the `sourceExpr flows targetName` construct, distinct from Lox's original expression types.

- **New statement types** in `Stmt.java`: `Root` (root river declarations with optional `f`/`s`/`m` flow-literal fields), `RiverDecl` (combination river declarations), `Dam` (a dam's full body of rules), `DamRule` (one `when`/`default` clause), and `Connect` (a standalone `flows` statement).

- **Parser methods** in `Parser.java` beyond Chapter 6's expression grammar: `riverDecl()`, `flowField()`, `flowExpr()`, `combination()`, `damDecl()`, `damRule()`, `defaultRule()`, `condition()`, `connectStmt()`, and `checkNext()` (a one-token lookahead helper needed to distinguish a bare `flows` statement from an expression statement without backtracking).

- **`AstPrinter.java`** was extended with visitor methods for every new expression and statement type, and its `wrapIfNested()` helper was written specifically to make nested `Binary`/`Flows` expressions visibly parenthesized in printed output, which is not something the book's original printer needed to handle.

- **Multi-file program concatenation** in `Weir.java`: the original Chapter 6 driver only ever ran one file per invocation. I added a `runFiles()` method, used whenever more than one file path is passed on the command line, which reads every file, concatenates their contents with newline separators (to prevent tokens merging across a file boundary if a trailing semicolon is missing), and calls the scanner/parser exactly **once** on the combined source. This means a river or dam declared in one `.weir` file is visible to statements in a second file passed in the same run, without needing any import or module system — the whole set of files is treated as one shared program. The one deliberate trade-off, documented directly in the code comments above `runFiles()`, is that error line numbers become relative to the combined source rather than resetting to 1 at each file boundary, since a single continuous token stream cannot also reset its own line counter at arbitrary internal points; the combined run is tagged with a joined filename (e.g. `brisbane.weir+sluicegate.weir`) so a marker can still see which invocation produced an error. This was verified working: passing `brisbane.weir` and `sluicegate.weir` together produced one continuous, correctly-ordered AST covering both files with no parse errors and no token corruption at the seam.

- **Explicit UTF-8 decoding** in `Weir.java`'s `readFile()`: rather than relying on `Charset.defaultCharset()` (which is what happens implicitly if you decode bytes without specifying a charset), I read file bytes and decode them with `StandardCharsets.UTF_8` explicitly. This matters because the JVM's default charset is platform-dependent — Windows can default to a different charset than macOS/Linux depending on system locale — so without this change, the exact same `.weir` source file could be scanned differently (or fail to scan correctly at all for non-ASCII characters) depending on which machine runs the interpreter.

## Uniqueness and Creativity (20)

What did you do beyond the in-class work?  Point your marker to where it lives in your submission.

### Your answer

Beyond my team's in-class workshop baseline, I made the following additions, all implemented and verified tonight:

- **Multi-file program support.** My interpreter accepts more than one `.weir` file per invocation and treats them as a single shared program — declarations in one file are visible to statements in another, with no import/module syntax required. This is implemented in `Weir.java`'s `runFiles()` method and was verified end-to-end by running `brisbane.weir` and `sluicegate.weir` together and confirming a single, correctly-ordered, error-free combined AST was produced. This lets a river system be split across multiple files by concern (e.g. one file for river/dam declarations, another for the flow statements that connect them), which is not something the base Lox driver or the in-class workshop code supports.

- **A dedicated dam-syntax showcase program**, `sluicegate.weir`, purpose-built (rather than tied to a real place) specifically to exercise every comparison operator my `condition` grammar rule supports (`>`, `>=`, `<`, `<=`, `==`, `!=`) across three chained dams, going beyond simply reusing the `>`-only comparisons shown in the assignment brief's own exemplar.

- **A second full real-world example**, `brisbane.weir`, built from the actual Brisbane River catchment (Somerset Dam and Wivenhoe Dam on the Stanley/Brisbane Rivers), rather than only submitting the Canberra example given in the brief. This shows the grammar generalising to an independently-researched, larger, and more structurally varied real system.

- **Explicit, portable UTF-8 handling** in file reading, rather than depending on the JVM's platform-default charset, so my language behaves identically regardless of which operating system it's compiled and run on.

These features live in: `Weir.java` (multi-file concatenation and UTF-8 decoding), and `sluicegate.weir` / `brisbane.weir` (the two example programs demonstrating the design choices above).