CREATE OR REPLACE VIEW missing_answers_link AS
with missing_answers as (
	select distinct question_number
	from answers
	where length(text) = 2 or number = 99
),
missing_answers_link as (
	select *
	from questionslink
	where number in (
		select * from missing_answers
	)
)
select * from missing_answers_link
order by number;