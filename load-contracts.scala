import java.io.File
import scala.util.Try
import org.anormcypher.{Cypher, Neo4jREST}

object LoadContracts extends App {

  println("""
    ___  __     __
   / _ \/ /_ __/ /____
  / ___/ / // / __/ _ \
 /_/  /_/\_,_/\__/\___/

  """)

  Neo4jREST.setServer("localhost")

  load(new File("awards.csv"))

  // TODO: deal with contracts awarded to multiple awardees (split on semicolon) -- will require Extract change (model contract as a node...)

  def load(file: File) = {
    val query = {
      s"LOAD CSV WITH HEADERS FROM 'file://${file.getAbsolutePath}' AS line" +
      """
      FOREACH(companyNumber IN (CASE WHEN line.awardeeCompanyNumber <> '' THEN [line.awardeeCompanyNumber] ELSE [] END) |
      MERGE (c:Organisation {name: line.orgName}) ON CREATE SET
        c.orgContact = line.orgContact
      MERGE (a:Organisation {companyNumber: companyNumber}) ON CREATE SET
        a.name = line.awardeeName
      CREATE (c)-[:AWARDED_CONTRACT_TO {
        noticeId: line.noticeId,
        noticeType: line.noticeType,
        noticeState: line.noticeState,
        noticeStateChangeDate: toInt(replace(line.noticeStateChangeDate, '-', '')),
        referenceNumber: line.referenceNumber,
        awardDate: toInt(replace(line.awardDate, '-', '')),
        publishedDate: toInt(replace(line.publishedDate, '-', '')),
        value: toInt(line.value),
        status: line.status,
        title: line.title,
        description: line.description,
        region: line.region,
        classification: line.classification,
        numDocs: line.numDocs
      }]->(a)
      )
      FOREACH(name IN (CASE WHEN line.awardeeCompanyNumber = '' THEN [line.awardeeName] ELSE [] END) |
      MERGE (c:Organisation {name: line.orgName}) ON CREATE SET
        c.orgContact = line.orgContact
      MERGE (a:Organisation {name: name})
      CREATE (c)-[:AWARDED_CONTRACT_TO {
        noticeId: line.noticeId,
        noticeType: line.noticeType,
        noticeState: line.noticeState,
        noticeStateChangeDate: toInt(replace(line.noticeStateChangeDate, '-', '')),
        referenceNumber: line.referenceNumber,
        awardDate: toInt(replace(line.awardDate, '-', '')),
        publishedDate: toInt(replace(line.publishedDate, '-', '')),
        value: toInt(line.value),
        status: line.status,
        title: line.title,
        description: line.description,
        region: line.region,
        classification: line.classification,
        numDocs: line.numDocs
      }]->(a)
      )
      """
    }
    println(query)
    Try(Cypher(query).apply()) recover {
      case e => println(s"FAILED TO LOAD AWARDS: \n${e.getMessage}")
    }
  }

}
