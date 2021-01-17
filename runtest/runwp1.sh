#rm -rf ../../wordpress
#cp -r ../..//WordPress-4.7.1 ../../WordPress-4.7.11
../../php-cs-fixer fix --rules=@PSR2 ../../WordPress-4.7.1
../../phpjoern/php2ast /home/users/chluo/WordPress-4.7.1
javac -cp "./bin:ApacheCommons/commons-cli-1.4/commons-cli-1.4.jar:ApacheCommons/commons-cli-1.4/commons-cli-1.4-sources.jar:ApacheCommons/commons-csv-1.8-bin/commons-csv-1.8/commons-csv-1.8.jar:ApacheCommons/commons-csv-1.8-bin/commons-csv-1.8/commons-csv-1.8-sources.jar:ApacheCommons/commons-lang3-3.10/commons-lang3-3.10.jar:ApacheCommons/commons-lang3-3.10/commons-lang3-3.10-sources.jar:ApacheCommons/json-20190722.jar" ./projects/extensions/joern-php/src/main/java/tools/php/ast2cpg/CommandLineInterface.java
mv ./projects/extensions/joern-php/src/main/java/tools/php/ast2cpg/CommandLineInterface.class ./bin/tools/php/ast2cpg/CommandLineInterface.class
javac -cp ./bin/ projects/extensions/joern-php/src/main/java/tools/php/ast2cpg/ParseCG.java
mv ./projects/extensions/joern-php/src/main/java/tools/php/ast2cpg/ParseCG.class ./bin/tools/php/ast2cpg/ParseCG.class
javac -cp "./bin:./ApacheCommons/json-20190722.jar" ./projects/extensions/joern-php/src/main/java/tools/php/ast2cpg/Debloat.java
mv ./projects/extensions/joern-php/src/main/java/tools/php/ast2cpg/Debloat.class ./bin/tools/php/ast2cpg/Debloat.class
javac -cp "./bin:./ApacheCommons/json-20190722.jar" ./projects/extensions/joern-php/src/main/java/cg/FileDependencyCheck.java
mv ./projects/extensions/joern-php/src/main/java/cg/FileDependencyCheck.class ./bin/cg/FileDependencyCheck.class 
javac -cp ./bin ./projects/extensions/joern-php/src/main/java/cg/PHPCGFactory.java 
mv ./projects/extensions/joern-php/src/main/java/cg/PHPCGFactory.class ./bin/cg/PHPCGFactory.class
java -Xmx4096M -Dfile.encoding=GBK -cp "./bin:ApacheCommons/commons-cli-1.4/commons-cli-1.4.jar:ApacheCommons/commons-cli-1.4/commons-cli-1.4-sources.jar:ApacheCommons/commons-csv-1.8-bin/commons-csv-1.8/commons-csv-1.8.jar:ApacheCommons/commons-csv-1.8-bin/commons-csv-1.8/commons-csv-1.8-sources.jar:ApacheCommons/commons-lang3-3.10/commons-lang3-3.10.jar:ApacheCommons/commons-lang3-3.10/commons-lang3-3.10-sources.jar:ApacheCommons/json-20190722.jar"  tools.php.ast2cpg.Main /home/users/chluo/WordPress-4.7.1
