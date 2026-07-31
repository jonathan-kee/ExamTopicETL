CREATE TABLE browserless_companies
(
    name TEXT,
    CONSTRAINT b_company_pk PRIMARY KEY (name)
);

INSERT INTO browserless_companies (name) VALUES ('Oracle');

CREATE TABLE browserless_exams
(
    name TEXT,
    company TEXT NOT NULL,
    CONSTRAINT b_exam_pk PRIMARY KEY (name),
    CONSTRAINT b_exam_company_fk FOREIGN KEY (company) REFERENCES browserless_companies(name) ON DELETE CASCADE
);

INSERT INTO browserless_exams (name, company) VALUES ('1z0-071', 'Oracle');

CREATE TABLE browserless_questions
(
    number INT,
    exam TEXT NOT NULL,
    text TEXT,
    CONSTRAINT b_question_pk PRIMARY KEY (number, exam),
    CONSTRAINT b_question_exam_fk FOREIGN KEY (exam) REFERENCES browserless_exams(name) ON DELETE CASCADE
);

CREATE TABLE browserless_answers
(
    number INT,
    question_number INT NOT NULL,
    question_exam TEXT NOT NULL,
    text TEXT,
    is_correct BOOLEAN,
    CONSTRAINT b_answer_pk PRIMARY KEY (number, question_number, question_exam),
    CONSTRAINT b_answer_question_fk FOREIGN KEY (question_number, question_exam)
        REFERENCES browserless_questions(number, exam) ON DELETE CASCADE
);

CREATE TABLE browserless_discussions
(
    number INT,
    question_number INT NOT NULL,
    question_exam TEXT NOT NULL,
    selected_answer TEXT,
    text TEXT,
    upvote INT,
    CONSTRAINT b_discussion_pk PRIMARY KEY (number, question_number, question_exam),
    CONSTRAINT b_discussion_question_fk FOREIGN KEY (question_number, question_exam)
        REFERENCES browserless_questions(number, exam) ON DELETE CASCADE
);