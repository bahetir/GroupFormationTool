package com.assessme.controller;

import com.assessme.auth.CurrentUserService;
import com.assessme.model.*;
import com.assessme.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Optional;

/**
 * @author: monil
 * Created on: 2020-07-14
 */
@RestController
@RequestMapping("/survey")
public class SurveyController {

    private final Logger logger = LoggerFactory.getLogger(SurveyController.class);

    private final SurveyService surveyService;
    private final CourseService courseService;
    private final CurrentUserService currentUserService;
    private final QuestionService questionService;

    public SurveyController(SurveyService surveyService) {

        this.surveyService = SurveyServiceImpl.getInstance();
        this.courseService = CourseServiceImpl.getInstance();
        this.currentUserService = CurrentUserService.getInstance();
        this.questionService = QuestionServiceImpl.getInstance();
    }

//    @GetMapping(value = "/create_survey")
//    public String createSurvey(Model model) {
//        return "create_survey";
//    }
//    public ModelAndView createSurvey(@RequestParam("courseCode") String courseCode){
//
//    }

    @PostMapping(value = "/create_survey")
    public ResponseEntity addSurvey(@ModelAttribute("survey") Survey survey) {

        logger.info("request:" + survey);

        logger.info("Calling API for creating a new survey.");
        HttpStatus httpStatus = null;
        ResponseDTO<User> responseDTO = null;

        try {
            Optional<Survey> newSurvey = surveyService.addSurvey(survey);
            String resMessage = String
                    .format("Survey :%s is created successfully for the Course id:%s by the user: %s",
                            survey.getSurveyName(), survey.getCourseId(), survey.getUserId());

            responseDTO = new ResponseDTO(true, resMessage, null, newSurvey.get());
            httpStatus = HttpStatus.OK;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());

            String errMessage = String.format("Error in creating survey");
            responseDTO = new ResponseDTO(false, errMessage, e.getLocalizedMessage(), null);
            httpStatus = HttpStatus.CONFLICT;
        }
        return new ResponseEntity(responseDTO, httpStatus);
   }

    @GetMapping(value = "/course_surveys")
    public ModelAndView getCourseSurveys(@RequestParam("courseCode") String courseCode){

        logger.info("Calling API for survey retrieval for the course: " + courseCode);
        HttpStatus httpStatus = null;
        ResponseDTO<List<Survey>> responseDTO = null;
        ModelAndView mav = new ModelAndView("survey_manager");
        try {
            Optional<Course> course = courseService.getCourseWithCode(courseCode);
            Long courseId = course.get().getCourseId();
            List<Survey> surveyList = surveyService.getSurveysForCourse(courseId);
            String resMessage = String.format("Survey list has been retrieved from the database");

            mav.addObject("surveyList", surveyList);
            mav.addObject("courseId", courseId);

            Survey survey = new Survey();
            survey.setCourseId(courseId);
            survey.setUserId(currentUserService.getAuthenticatedUser().get().getUserId());
            survey.setStatus("unpublished");
            mav.addObject("survey",survey);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());

            String errMessage = String.format("Error in retrieving the survey from the database");
            logger.error("Error fetching survey_manager page");
            mav.addObject("message", errMessage);
        }
        return mav;
    }

    @PutMapping(value = "/change_status" , consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ResponseDTO> updateSurveyStatus(@RequestBody Survey survey){

        logger.info("Calling API for updating survey the survey: " + survey);
        HttpStatus httpStatus = null;
        ResponseDTO<List<Survey>> responseDTO = null;

        try {
            Optional<Survey> updatedSurvey = surveyService.updateSurveyStatus(survey);
            String resMessage = String.format("Survey has been updated in the system");
            responseDTO = new ResponseDTO(true, resMessage, null, updatedSurvey.get());
            httpStatus = HttpStatus.OK;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());

            String errMessage = String.format("Error in updating the survey to the database");
            responseDTO = new ResponseDTO(false, errMessage, e.getLocalizedMessage(), null);
            httpStatus = HttpStatus.CONFLICT;
        }

        return new ResponseEntity(responseDTO, httpStatus);
    }

    @GetMapping("/survey_page/{surveyId}")
    public ModelAndView surveyPage(@PathVariable long surveyId){
        logger.info("in controller method surveyPage");
        ModelAndView mav = new ModelAndView("survey_questions");
        try{
            mav.addObject("survey_id", surveyId);
            logger.info("after adding survey id in model");
        }catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());

            String errMessage = String.format("Error in retrieving the survey questions from the database");
            logger.error("Error fetching questions for survey_page page");
            mav.addObject("message", errMessage);
        }
        return mav;
    }

    @GetMapping("/user_questions")
    public ModelAndView getQuestions(@RequestParam long surveyId){
        SurveyQuestionsDTO surveyQuestionsDTO = new SurveyQuestionsDTO();
        ModelAndView mav = new ModelAndView("user_questions");
        mav.addObject("survey_id", surveyId);
        mav.addObject("surveyQuestionsDTO", surveyQuestionsDTO);
        try {
            logger.info("in user_questions endpoint of survey controller");
            User user = currentUserService.getAuthenticatedUser().get();
            logger.info(String.format("Getting Questions for User %d", user.getUserId()));
            List<Question> questionsByUser = questionService.getQuestionsByUser(user).get();
            mav.addObject("questions", questionsByUser);
        } catch (Exception e) {
            logger.error("Error Getting Questions for User");
            mav.addObject("message", "Error Fetching Page");
        }
        return mav;
    }
}
