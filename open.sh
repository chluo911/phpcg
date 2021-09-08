#../php-cs-fixer fix --rules=@PSR2 ../goal/oscommerce2-2.3.4.1
../phpjoern/php2ast /home/users/chluo/goal/opencart
javac -cp ./bin projects/extensions/joern-php/src/main/java/tools/php/ast2cpg/StaticAnalysis.java
rm ./bin/tools/php/ast2cpg/StaticAnalysis.class
mv projects/extensions/joern-php/src/main/java/tools/php/ast2cpg/StaticAnalysis.class ./bin/tools/php/ast2cpg/StaticAnalysis.class
rm 1.csv
java -Xmx8G -cp "./bin:ApacheCommons/commons-cli-1.4/commons-cli-1.4.jar:ApacheCommons/commons-cli-1.4/commons-cli-1.4-sources.jar:ApacheCommons/commons-csv-1.8-bin/commons-csv-1.8/commons-csv-1.8.jar:ApacheCommons/commons-csv-1.8-bin/commons-csv-1.8/commons-csv-1.8-sources.jar:ApacheCommons/commons-lang3-3.10/commons-lang3-3.10.jar:ApacheCommons/commons-lang3-3.10/commons-lang3-3.10-sources.jar:ApacheCommons/json-20190722.jar"  tools.php.ast2cpg.Main /home/users/chluo/goal/opencart > 1.csv
