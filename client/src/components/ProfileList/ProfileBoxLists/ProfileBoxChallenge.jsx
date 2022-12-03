import { ProfileBoxChallengeComponent } from '../../../style/MyProfilePageStyle/ProfileBoxListsStyle/ProfileBoxListsStyle';
//반복되는 대상 맵 필요x

function ProfileBoxList({
  memberReward,
  challengeTitle,
  clickedTab,
  challengeRepImage,
  memberSuccessDay,
  objDay,
  memberChallengeSuccessRate,
}) {
  if (ProfileBoxList === []) {
    return 'dkdk';
  }
  // 문제 => 각각의 탭 잘 나오다 0번째 탭을 클릭하고 이동하면 모두 0번 째 내용으로만 나옴

  return (
    <ProfileBoxChallengeComponent>
      <div className="title">
        <h1>
          {clickedTab === 0 && '도전 중'}
          {clickedTab === 1 && '도전내역'}
          {clickedTab === 2 && '결제내역'}
          {clickedTab === 0 && <span className="tag">NOW</span>}
          {/* 도전중 내전 내역 통합 */}
        </h1>
        <span className="percentage">{memberChallengeSuccessRate}%</span>
      </div>
      <form className="challenge-box">
        <img
          src={challengeRepImage}
          className="challenge-box-image"
          alt="challenge-box-img"
        />
        <div className="challenge-box-info">
          <p>{challengeTitle}</p>
          <div className="challenge-box-info-lists">
            <p>
              성공일수:{memberSuccessDay} / 목표일수:{objDay}
            </p>
            <p>예상환급금:{memberReward}원</p>
          </div>
        </div>
      </form>
      <p className="notice">
        *수수료로 인해 예상금액과 실제 환급금액은 서로 상이할 수 있음.
      </p>
    </ProfileBoxChallengeComponent>
  );
}

export default ProfileBoxList;