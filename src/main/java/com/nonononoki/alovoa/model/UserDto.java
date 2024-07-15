package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.*;
import com.nonononoki.alovoa.rest.MediaController;
import com.nonononoki.alovoa.service.UserService;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class UserDto {
    public static final int LA_STATE_ACTIVE_1 = 5; // in minutes
    public static final int LA_STATE_ACTIVE_2 = 1;
    public static final int LA_STATE_ACTIVE_3 = 7;
    public static final int LA_STATE_ACTIVE_4 = 30;
    public static final int VERIFICATION_MINIMUM = 5;
    public static final int VERIFICATION_FACTOR = 5;
    private static final double MILES_TO_KM = 0.6214;
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDto.class);

    private UUID uuid;
    private String email;
    private String firstName;
    private int age;
    private float donationAmount;
    private Gender gender;
    private boolean hasAudio;
    private String audio;
    private String zodiac;
    private boolean showZodiac;
    private int units;
    private int preferedMinAge;
    private int preferedMaxAge;
    private Set<UserMiscInfo> miscInfos;
    private Set<Gender> preferedGenders;
    private UserIntention intention;
    private List<UserInterest> interests;
    private List<UserInterest> commonInterests;
    private List<UserPrompt> prompts;
    private String profilePicture;
    private List<UserImageDto> images;
    private String description;
    private String country;
    private int distanceToUser;
    private double totalDonations;
    private long numBlockedByUsers;
    private long numReports;
    private boolean blockedByCurrentUser;
    private boolean reportedByCurrentUser;
    private boolean likesCurrentUser;
    private boolean likedByCurrentUser;
    private boolean hiddenByCurrentUser;
    private long numberReferred;
    private long numberProfileViews;
    private boolean compatible;
    private boolean hasLocation;
    private Double locationLatitude;
    private Double locationLongitude;
    private UserDtoVerificationPicture verificationPicture;
    private int lastActiveState = 5;
    private UserSettings userSettings;

    public static UserDto userToUserDto(DtoBuilder builder)
        throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
        NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {

    if (builder.user == null) {
        return null;
    }

    UserDto dto = new UserDto();

    if (builder.user.equals(builder.currentUser)) {
        populateCurrentUserDetails(dto, builder.user);
    }

    UUID uuid = Tools.getUserUUID(builder.user, builder.userService);
    dto.setUuid(uuid);

    populateUserBasicDetails(dto, builder.user, builder.userService, builder.currentUser, uuid);
    populateUserCompatibilityDetails(dto, builder.user, builder.currentUser, builder.userService, builder.ignoreIntention);

    return dto;
}

private static void populateCurrentUserDetails(UserDto dto, User user) {
    dto.setEmail(user.getEmail());
    dto.setLocationLatitude(user.getLocationLatitude());
    dto.setLocationLongitude(user.getLocationLongitude());
    dto.setUserSettings(user.getUserSettings());
}

private static void populateUserBasicDetails(UserDto dto, User user, UserService userService, User currentUser, UUID uuid) 
        throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
        NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {

    if (user.getDates() != null) {
        dto.setAge(Tools.calcUserAge(user));
    }
    dto.setHasLocation(user.getLocationLatitude() != null);
    dto.setDescription(user.getDescription());
    dto.setFirstName(user.getFirstName());
    dto.setGender(user.getGender());
    dto.setVerificationPicture(UserDtoVerificationPicture.map(user, currentUser, userService, user.getVerificationPicture().getUuid()));
    dto.setCountry(Tools.getCountryEmoji(user.getCountry()));
    dto.setShowZodiac(user.isShowZodiac());
    dto.setZodiac(currentUser.isShowZodiac() ? getUserZodiac(user) : null);
    dto.setUnits(user.getUnits());
    dto.setMiscInfos(user.getMiscInfos());
    dto.setPreferedGenders(user.getPreferedGenders());
    dto.setPreferedMinAge(Math.max(user.getPreferedMinAge(), (dto.getAge() >= Tools.AGE_LEGAL ? Tools.AGE_LEGAL : dto.getPreferedMinAge())));
    dto.setPreferedMaxAge(user.getPreferedMaxAge());
    dto.setImages(UserImageDto.buildFromUserImages(user, userService));
    dto.setProfilePicture(getUserProfilePicture(user, userService));
    dto.setTotalDonations(user.getTotalDonations());
    dto.setNumBlockedByUsers(user.getBlockedByUsers().size());
    dto.setNumReports(user.getReportedByUsers().size());
    dto.setInterests(user.getInterests());
    dto.setAudio(user.getAudio() != null ? userService.getDomain() + MediaController.URL_REQUEST_MAPPING + MediaController.URL_AUDIO + (user.getAudio().getUuid() != null ? user.getAudio().getUuid() : uuid) : null);
    dto.setHasAudio(user.getAudio() != null);
    dto.setNumberReferred(user.getNumberReferred());
    dto.setPrompts(user.getPrompts());
}

private static String getUserProfilePicture(User user, UserService userService) throws UnsupportedEncodingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
    return user.getProfilePicture() != null ? UserProfilePicture.getPublicUrl(userService.getDomain(), Tools.getProfilePictureUUID(user.getProfilePicture(), userService)) : null;
}

private static void populateUserCompatibilityDetails(UserDto dto, User user, User currentUser, UserService userService, boolean ignoreIntention) {
    if (!user.equals(currentUser)) {
        dto.setBlockedByCurrentUser(isUserBlockedByCurrentUser(currentUser, user));
        dto.setReportedByCurrentUser(isUserReportedByCurrentUser(currentUser, user));
        dto.setLikesCurrentUser(isUserLikedByCurrentUser(user, currentUser));
        dto.setLikedByCurrentUser(isUserLikedByCurrentUser(currentUser, user));
        dto.setHiddenByCurrentUser(isUserHiddenByCurrentUser(currentUser, user));
        dto.setCommonInterests(getCommonInterests(currentUser, user));
        dto.setDistanceToUser(getDistanceToUser(user, currentUser, userService));
    }
    dto.setCompatible(Tools.usersCompatible(currentUser, user, ignoreIntention));
    if (!user.isAdmin()) {
        dto.setLastActiveState(getLastActiveState(user));
    }
}

private static boolean isUserBlockedByCurrentUser(User currentUser, User user) {
    return currentUser.getBlockedUsers().stream().filter(o -> o.getUserTo() != null).anyMatch(o -> Objects.equals(o.getUserTo().getId(), user.getId()));
}

private static boolean isUserReportedByCurrentUser(User currentUser, User user) {
    return currentUser.getReported().stream().filter(o -> o.getUserTo() != null).anyMatch(o -> Objects.equals(o.getUserTo().getId(), user.getId()));
}

private static boolean isUserLikedByCurrentUser(User user, User currentUser) {
    return user.getLikes().stream().filter(o -> o.getUserTo() != null).anyMatch(o -> Objects.equals(o.getUserTo().getId(), currentUser.getId()));
}

private static boolean isUserHiddenByCurrentUser(User currentUser, User user) {
    return currentUser.getHiddenUsers().stream().filter(o -> o.getUserTo() != null).anyMatch(o -> Objects.equals(o.getUserTo().getId(), user.getId()));
}

private static List<UserInterest> getCommonInterests(User currentUser, User user) {
    List<UserInterest> commonInterests = new ArrayList<>();
    for (UserInterest interest : currentUser.getInterests()) {
        if (user.getInterests().contains(interest)) {
            commonInterests.add(interest);
        }
    }
    return commonInterests;
}

private static int getDistanceToUser(User user, User currentUser, UserService userService) {
    int dist = 99999;
    if (!currentUser.isAdmin()) {
        dist = Tools.getDistanceToUser(user, currentUser);
        if (currentUser.getUnits() == User.UNIT_IMPERIAL) {
            dist = (int) (dist * MILES_TO_KM);
        }
    }
    return dist;
}

private static int getLastActiveState(User user) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime activeDateTime = Tools.dateToLocalDateTime(user.getDates().getActiveDate());
    if (activeDateTime.isAfter(now.minusMinutes(LA_STATE_ACTIVE_1))) {
        return 1;
    } else if (activeDateTime.isAfter(now.minusDays(LA_STATE_ACTIVE_2))) {
        return 2;
    } else if (activeDateTime.isAfter(now.minusDays(LA_STATE_ACTIVE_3))) {
        return 3;
    } else if (activeDateTime.isAfter(now.minusDays(LA_STATE_ACTIVE_4))) {
        return 4;
    } else {
        return 0;
    }
}


    public static long decodeIdThrowing(String id, TextEncryptorConverter textEncryptor)
            throws NumberFormatException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        String en = new String(Base64.getDecoder().decode(id));
        return Long.parseLong(textEncryptor.decode(en));
    }

    @Deprecated
    public static Optional<Long> decodeId(String id, TextEncryptorConverter textEncryptor) {
        try {
            String en = new String(Base64.getDecoder().decode(id));
            return Optional.of(Long.parseLong(textEncryptor.decode(en)));
        } catch (Exception e) {
            LOGGER.debug(String.format("Couldn't decode id '%s'", id), e);
        }
        return Optional.empty();
    }


    public static String getUserZodiac(User user) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(user.getDates().getDateOfBirth());
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        if (month == 12 && day >= 22 || month == 1 && day <= 19)
            return "capricorn";
        else if (month == 1 || month == 2 && day <= 17)
            return "aquarius";
        else if (month == 2 && day <= 29 || month == 3 && day <= 19)
            return "pisces";
        else if (month == 3 || month == 4 && day <= 19)
            return "aries";
        else if (month == 4 && day <= 30 || month == 5 && day <= 20)
            return "taurus";
        else if (month == 5 || month == 6 && day <= 20)
            return "gemini";
        else if (month == 6 && day <= 30 || month == 7 && day <= 22)
            return "cancer";
        else if (month == 7 || month == 8 && day <= 22)
            return "leo";
        else if (month == 8 || month == 9 && day <= 22)
            return "virgo";
        else if (month == 9 && day <= 30 || month == 10 && day <= 22)
            return "libra";
        else if (month == 10 || month == 11 && day <= 21)
            return "scorpio";
        else if (month == 11 && day <= 30 || month == 12)
            return "sagittarius";
        return null;

    }

    public static boolean isVerifiedByUsers(UserVerificationPicture pic) {
        if (pic.getUserYes().size() < VERIFICATION_MINIMUM) {
            return false;
        }
        return pic.getUserNo().size() * VERIFICATION_FACTOR <= pic.getUserYes().size();
    }

    @Data
    public static class UserDtoVerificationPicture {
        private boolean verifiedByAdmin;
        private boolean verifiedByUsers;
        private boolean votedByCurrentUser;
        private boolean hasPicture;
        private String data;
        private String text;
        private UUID uuid;
        private int userYes;
        private int userNo;

        public static UserDtoVerificationPicture map(User user, User currentUser, UserService userService, UUID uuid) {
            UserDtoVerificationPicture verificationPicture = new UserDtoVerificationPicture();
            verificationPicture.setText(userService.getVerificationCode(user));
            UserVerificationPicture pic = user.getVerificationPicture();
            verificationPicture.setHasPicture(pic != null && pic.getBin() != null);

            if (pic == null) {
                return verificationPicture;
            }

            UUID picUuid = pic.getUuid() != null ? pic.getUuid() : uuid;

            if (!pic.isVerifiedByAdmin()) {
                verificationPicture.setData(UserVerificationPicture.getPublicUrl(userService.getDomain(), uuid));
            }

            //only show verification for users with verification
            if (currentUser == user || currentUser.getVerificationPicture() == null && !currentUser.isAdmin()) {
                return verificationPicture;
            }

            verificationPicture.setUserNo(pic.getUserNo().size());
            verificationPicture.setUserYes(pic.getUserYes().size());
            verificationPicture.setVerifiedByUsers(UserDto.isVerifiedByUsers(pic));
            verificationPicture.setVerifiedByAdmin(pic.isVerifiedByAdmin());
            verificationPicture.setVotedByCurrentUser(pic.getUserYes().contains(currentUser) || pic.getUserNo().contains(currentUser));
            verificationPicture.setUuid(picUuid);

            return verificationPicture;
        }
    }

    @Builder
    public static class DtoBuilder {
        private User user;
        private User currentUser;
        private UserService userService;
        private boolean ignoreIntention;
    }
}
