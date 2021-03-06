import java.io.File
import scala.io.StdIn
import scala.math.BigDecimal
import org.anormcypher.{Cypher, Neo4jREST}
import com.github.tototoshi.csv.{CSVReader, CSVWriter}
import Common._

/*

Querying uses Neo4j legacy indexing for full-text search until something better arrives.
To set it up:

 $ curl -vX POST -H 'Content-Type: application/json' -d '{"name":"node_auto_index","config":{"type":"fulltext","provider":"lucene"}}' localhost:7474/db/data/index/node/
    (if you get an error try rm -rf /usr/local/Cellar/neo4j/2.1.5/libexec/data/graph.db/index*)
 $ sed -i bak 's/#node_auto_indexing/node_auto_indexing/g' /usr/local/Cellar/neo4j/2.1.5/libexec/conf/neo4j.properties
 $ sed -i bak 's/#node_keys_indexable/node_keys_indexable/g' /usr/local/Cellar/neo4j/2.1.5/libexec/conf/neo4j.properties
 $ neo4j restart
 $ curl -vX POST -H 'Content-Type: application/json' -d '{"query":"MATCH (n) WHERE has(n.name) SET n.name=n.name"}' localhost:7474/db/data/cypher

*/

object Query extends App {

  Neo4jREST.setServer("localhost")

  run()

  def run() {
    val query = StdIn.readLine("Query name:\n=> ") match {
      case "direct" => queryDirectDonations
      case "indirect" => queryIndirectDonations
      case "received" => queryReceived
      case "top-donors" => queryTopDonors
      case "top-corporate-donors" => queryTopCorporateDonors
      case "top-donations" => queryTopDonations
      case "new-donors" => queryNewDonors
      case _ => sys.exit()
    }
    val outputFile = StdIn.readLine("Output file:\n=> ")
    val output = CSVWriter.open(new File(outputFile))
    output.writeAll(query)
  }

  def inputBatch(key: String)(block: String => List[String]): List[List[String]] = {
    val inputFile = StdIn.readLine("Input file:\n=> ")
    val input = CSVReader.open(new File(inputFile)).allWithHeaders
    input map { entry =>
      val keyValue = entry(key)
      block(keyValue)
    }
  }

  def luceneName(name: String) = {
    val terms = tidy(stripTitles(name)).split(" ").filter(_.length >= 2)
    terms.map("name:\"" + _ + "\"").mkString(" AND ")
  }

  /*
    Match names of either company or individual donors, and return
    the total amount and number of donations they made
   */
  def queryDirectDonations(): List[List[String]] = {
    inputBatch("Name") { name =>
      val query = {
        s"""
          START b=node:node_auto_index('${luceneName(name)}')
          MATCH (b)-[d:DONATED_TO]->(r)
          RETURN
            collect(DISTINCT b.name) AS matchedNames,
            collect(DISTINCT b.companyNumber) AS matchedCompanyNumbers,
            collect(DISTINCT r.name) AS recipients,
            count(d) AS donationsCount,
            sum(d.value) / 100.0 AS donationsTotal
        """
      }
      val results = Cypher(query).apply() flatMap { row =>
        List(
          row[Seq[String]]("matchedNames").mkString("; "),
          row[Seq[String]]("matchedCompanyNumbers").mkString("; "),
          row[Seq[String]]("recipients").mkString("; "),
          row[Int]("donationsCount").toString,
          row[BigDecimal]("donationsTotal").toString
        )
      }
      name :: results.toList
    }
  }

  /*
    Match names of those who sit on the board of companies who have donated,
    and return the total amount and number of donations the company made
   */
  def queryIndirectDonations(): List[List[String]] = {
    inputBatch("Name") { name =>
      val query = {
        s"""
        START b=node:node_auto_index('${luceneName(name)}')
        MATCH (b)-[iaoo:IS_AN_OFFICER_OF]->(o)-[d:DONATED_TO]->(r)
        RETURN
          collect(DISTINCT b.name) AS matchedNames,
          collect(DISTINCT iaoo.position) AS matchedPositions,
          collect(DISTINCT o.name) AS matchedCompanyNames,
          collect(DISTINCT o.companyNumber) AS matchedCompanyNumbers,
          collect(DISTINCT r.name) AS recipients,
          count(d) AS donationsCount,
          sum(d.value) / 100.0 AS donationsTotal
        """
      }
      val results = Cypher(query).apply() flatMap { row =>
        List(
          row[Seq[String]]("matchedNames").mkString("; "),
          row[Seq[String]]("matchedPositions").mkString("; "),
          row[Seq[String]]("matchedCompanyNames").mkString("; "),
          row[Seq[String]]("matchedCompanyNumbers").mkString("; "),
          row[Seq[String]]("recipients").mkString("; "),
          row[Int]("donationsCount").toString,
          row[BigDecimal]("donationsTotal").toString
        )
      }
      name :: results.toList
    }
  }

  /*
    The total amount donated to each party -- accepted (not received or reported!) since the given date
    Doesn't include public funds!
   */
  def queryReceived(): List[List[String]] = {
    val date = StdIn.readLine("From date (yyyy-mm-dd):\n=> ").replace("-", "")
    val query = {
      s"""
        MATCH (b)-[d:DONATED_TO]->(r:Party)
        WHERE d.acceptedDate >= $date
        AND d.type <> 'Public Funds'
        RETURN
          r.name AS recipient,
          count(d) AS donationsCount,
          sum(d.value) / 100.0 AS donationsTotal
        ORDER BY donationsTotal DESC
      """
    }
    val results = Cypher(query).apply() map { row =>
      List(
        row[String]("recipient"),
        row[Int]("donationsCount").toString,
        row[BigDecimal]("donationsTotal").toString
      )
    }
    results.toList
  }

  /*
    The top ten donors -- accepted (not received or reported!) since the given date
    Doesn't include public funds!
   */
  def queryTopDonors(): List[List[String]] = {
    val date = StdIn.readLine("From date (yyyy-mm-dd):\n=> ").replace("-", "")
    val query = {
      s"""
        MATCH (b)-[d:DONATED_TO]->(r:Party)
        WHERE d.acceptedDate >= $date
        AND d.type <> 'Public Funds'
        RETURN
          b.name AS benefactor,
          r.name AS recipient,
          count(d) AS donationsCount,
          sum(d.value) / 100.0 AS donationsTotal
        ORDER BY donationsTotal DESC
        LIMIT 10
      """
    }
    val results = Cypher(query).apply() map { row =>
      List(
        row[String]("benefactor"),
        row[String]("recipient"),
        row[Int]("donationsCount").toString,
        row[BigDecimal]("donationsTotal").toString
      )
    }
    results.toList
  }

  /*
    The top ten corporate donors to the given party -- accepted (not received or reported!) since the given date
   */
  def queryTopCorporateDonors(): List[List[String]] = {
    val date = StdIn.readLine("From date (yyyy-mm-dd):\n=> ").replace("-", "")
    val party = StdIn.readLine("Party:\n=> ")
    val query = {
      s"""
        MATCH (b:Organisation)-[d:DONATED_TO]->(r:Party)
        WHERE d.acceptedDate >= $date
        AND d.type <> 'Public Funds'
        AND r.name = '$party'
        RETURN
          b.name AS benefactor,
          count(d) AS donationsCount,
          sum(d.value) / 100.0 AS donationsTotal
        ORDER BY donationsTotal DESC
        LIMIT 10
      """
    }
    val results = Cypher(query).apply() map { row =>
      List(
        row[String]("benefactor"),
        row[Int]("donationsCount").toString,
        row[BigDecimal]("donationsTotal").toString
      )
    }
    results.toList
  }

  /*
    The top ten individual donations by size -- accepted (not received or reported!) since the given date
    Doesn't include public funds!
   */
  def queryTopDonations(): List[List[String]] = {
    val date = StdIn.readLine("From date (yyyy-mm-dd):\n=> ").replace("-", "")
    val query = {
      s"""
        MATCH (b)-[d:DONATED_TO]->(r:Party)
        WHERE d.acceptedDate >= $date
        AND d.type <> 'Public Funds'
        RETURN
          b.name AS benefactor,
          r.name AS recipient,
          d.acceptedDate AS date,
          d.value / 100.0 AS donationAmount
        ORDER BY donationAmount DESC
        LIMIT 10
      """
    }
    val results = Cypher(query).apply() map { row =>
      List(
        row[String]("benefactor"),
        row[String]("recipient"),
        row[Int]("date").toString,
        row[BigDecimal]("donationAmount").toString
      )
    }
    results.toList
  }


  /*
    List new donors (who donated since the given date)
   */
  def queryNewDonors(): List[List[String]] = {
    val date = StdIn.readLine("From date (yyyy-mm-dd):\n=> ").replace("-", "")
    val query = {
      s"""
        MATCH (b)-[d:DONATED_TO]->(r)
        WITH b, count(d) as donationCount, collect(d) AS ds, collect(r) AS rs
        WHERE donationCount = 1
        WITH b, head(ds) AS d, head(rs) AS r
        WHERE d.acceptedDate >= $date
        RETURN
          b.name AS benefactor,
          r.name AS recipient,
          d.value / 100.0 AS amount,
          d.ecReference AS ecReference
        ORDER BY amount DESC
      """
    }
    val results = Cypher(query).apply() map { row =>
      List(
        row[String]("benefactor"),
        row[String]("recipient"),
        row[BigDecimal]("amount").toString,
        row[String]("ecReference")
      )
    }
    results.toList
  }

}
