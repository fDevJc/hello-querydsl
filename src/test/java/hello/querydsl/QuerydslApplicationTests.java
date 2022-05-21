package hello.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import hello.querydsl.entity.HelloEntity;
import hello.querydsl.entity.QHelloEntity;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

	@PersistenceContext
	EntityManager em;

	@Test
	void contextLoads() {
		HelloEntity helloEntity = new HelloEntity();
		em.persist(helloEntity);

		JPAQueryFactory query = new JPAQueryFactory(em);
		QHelloEntity qHello = new QHelloEntity("h");

		HelloEntity entity = query
				.selectFrom(qHello)
				.fetchOne();

		Assertions.assertThat(entity).isEqualTo(helloEntity);
	}

}
