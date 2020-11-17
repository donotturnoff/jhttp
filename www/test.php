<!DOCTYPE html>

<html>
 <head>
  <title>Form output</title>
 </head>
 <body>
  <?php
$text = $_POST["text"];
if ($text == "hello") {
	echo "<h1>Hello</h1>";
} else {
	echo "<p>You entered: " . $_POST["text"] . "</p>";
}
  ?> 
 </body>
</html>
