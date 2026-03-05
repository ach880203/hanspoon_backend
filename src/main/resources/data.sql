-- src/main/resources/data.sql
-- 서버 구동 시 초기 데이터(Notice) 주입용 스크립트
-- 주의: hibernate.ddl-auto=update 상태에서는 테이블이 먼저 생성된 후 이 스크립트가 실행되어야 함. (defer-datasource-initialization: true 필요)

INSERT INTO notice (title, content, is_important, view_count, created_at) 
VALUES (
    '[필독] 한스푼 서비스 이용 약관 개정 안내', 
    '<h3>한스푼 서비스 이용 약관 개정 안내</h3><p>안녕하세요. 한스푼입니다.</p><p>항상 한스푼을 이용해 주시는 회원 여러분께 진심으로 감사드립니다.<br>한스푼 서비스 이용 약관이 다음과 같이 개정될 예정이오니, 이용에 참고해 주시기 바랍니다.</p><ul><li><strong>개정 사유:</strong> 신규 서비스 출시에 따른 약관 현행화</li><li><strong>적용 일자:</strong> 2026년 3월 15일</li></ul><p>앞으로도 더 나은 서비스를 제공하기 위해 최선을 다하겠습니다.<br>감사합니다.</p>', 
    1, 0, NOW()
) ON DUPLICATE KEY UPDATE title = title;

INSERT INTO notice (title, content, is_important, view_count, created_at) 
VALUES (
    '시스템 정기 점검 안내 (매월 1일 새벽 2시~4시)', 
    '<h3>시스템 정기 점검 안내</h3><p>안녕하세요. 한스푼 운영팀입니다.</p><p>안정적이고 원활한 서비스 제공을 위해 아래와 같이 <strong>시스템 정기 점검</strong>을 실시합니다.<br>점검 시간 동안에는 한스푼 서비스 접속 및 이용이 일시적으로 제한되오니 양해 부탁드립니다.</p><ul><li><strong>점검 일시:</strong> 매월 1일 02:00 ~ 04:00 (약 2시간)</li><li><strong>점검 내용:</strong> 서버 안정화 및 DB 최적화 작업</li></ul><p>이용에 불편을 드려 죄송합니다. 더 좋은 서비스로 보답하겠습니다.</p>', 
    0, 0, NOW()
) ON DUPLICATE KEY UPDATE title = title;


INSERT INTO notice (title, content, is_important, view_count, created_at) 
VALUES (
    '[안내] 고객센터 전화상담 운영시간 변경 안내', 
    '<h3>📞 고객센터 전화상담 운영시간 변경 안내</h3><p>안녕하세요. 한스푼입니다.</p><p>고객 여러분께 더 나은 맞춤형 상담을 제공하기 위해, <strong>2026년 3월 1일</strong>부터 고객센터 전화상담 운영 시간이 아래와 같이 변경됩니다.</p><ul><li><strong>기존:</strong> 평일 09:00 ~ 18:00</li><li><strong>변경:</strong> 평일 10:00 ~ 17:00 (점심시간 12:30 ~ 13:30 제외)</li></ul><p>게시판 및 1:1 문의는 기존과 동일하게 24시간 접수 가능하오니 많은 이용 바랍니다.<br>감사합니다.</p>', 
    0, 0, NOW()
) ON DUPLICATE KEY UPDATE title = title;

INSERT INTO notice (title, content, is_important, view_count, created_at) 
VALUES (
    '새로운 간편 결제 수단 추가 안내 (카카오페이/토스페이)', 
    '<h3>💳 간편 결제 수단 (카카오페이, 토스페이) 추가 안내</h3><p>안녕하세요. 한스푼입니다.</p><p>클래스 예약 및 마켓 상품 구매 시 더욱 빠르고 편리하게 결제하실 수 있도록, 새로운 간편 결제 수단이 추가되었습니다!</p><ul><li><strong>오픈 일시:</strong> 2026년 3월 2일 (월) 오전 10시</li><li><strong>추가 수단:</strong> 카카오페이, 토스페이</li></ul><p>이제 복잡한 카드 인증 없이 비밀번호 6자리 또는 생체 인증만으로 한스푼의 모든 서비스를 이용해 보세요.<br>감사합니다.</p>', 
    0, 0, NOW()
) ON DUPLICATE KEY UPDATE title = title;

INSERT INTO notice (title, content, is_important, view_count, created_at) 
VALUES (
    '[모집] 한스푼 맛칼럼니스트 & 공식 서포터즈 1기 모집', 
    '<h3>📝 한스푼 공식 서포터즈 1기 대모집!</h3><p>요리에 진심인 분들을 찾습니다! 한스푼과 함께 건강하고 맛있는 요리 문화를 만들어갈 <strong>공식 서포터즈 1기</strong>에 지원하세요.</p><ul><li><strong>모집 기간:</strong> 2026년 3월 5일 ~ 2026년 3월 20일</li><li><strong>활동 혜택:</strong> 매월 5만 스푼 지급, 한스푼 굿즈 패키지 증정, 우수 활동자 특별 포상</li><li><strong>지원 방법:</strong> 마이페이지 > 이벤트 탭에서 서포터즈 지원서 작성 및 제출</li></ul><p>나의 요리 레시피를 전 세계와 나누고 싶은 분들의 많은 참여를 기다립니다! 👨‍🍳👩‍🍳</p>', 
    1, 0, NOW()
) ON DUPLICATE KEY UPDATE title = title;

INSERT INTO notice (title, content, is_important, view_count, created_at) 
VALUES (
    '개인정보 처리방침 일부 개정 사전 안내', 
    '<h3>🔒 개인정보 처리방침 개정 안내</h3><p>안녕하세요. 한스푼입니다.</p><p>한스푼을 이용해 주시는 회원님들께 감사드리며, 당사 개인정보 처리방침이 일부 개정될 예정이오니 변경된 내용을 확인해 주시기 바랍니다.</p><ul><li><strong>주요 개정 내용:</strong> 제휴사 추가에 따른 제3자 정보 제공 항목 업데이트</li><li><strong>개정 시기:</strong> 2026년 3월 25일부터 효력 발생</li></ul><p>본 개정 내용에 동의하지 않으실 경우, 개정 전일까지 회원 탈퇴를 요청하실 수 있습니다.<br>한스푼은 앞으로도 회원님의 소중한 개인정보를 안전하게 보호하겠습니다.</p>', 
    0, 0, NOW()
    0, 0, NOW()
) ON DUPLICATE KEY UPDATE title = title;

-- ==========================================
-- 이벤트(Event) 전용 초기 데이터 주입 스크립트
-- ==========================================

-- 1. 진행 중인 이벤트 (가상의 썸네일 이미지 링크 포함)
INSERT INTO event (title, content, thumbnail_url, start_date, end_date, view_count, created_at)
VALUES (
    '[이벤트] 신규 가입 3,000 스푼 100% 당첨 이벤트!', 
    '<h3>🎉 신규 가입 3,000 스푼 100% 당첨 이벤트!</h3><img src="/img/event-new-member.jpg" alt="event illustration" /><p>지금 가입하시면, 요리 클래스부터 레시피 열람까지 자유롭게 사용할 수 있는 <strong>3,000 스푼(포인트)</strong>을 즉시 지급해 드립니다!</p><ul><li><strong>참여 대상:</strong> 신규 가입 회원 전원</li><li><strong>지급 내용:</strong> 3,000 스푼 쿠폰</li><li><strong>유의 사항:</strong> 지급일로부터 30일 이내 사용 가능</li></ul>',
    'https://images.unsplash.com/photo-1513201099345-c2e9113ef0fa?auto=format&fit=crop&q=80&w=500',
    DATE_SUB(NOW(), INTERVAL 5 DAY), 
    DATE_ADD(NOW(), INTERVAL 30 DAY), 
    0, NOW()
);

-- 2. 진행 중인 이벤트 (맛칼럼니스트)
INSERT INTO event (title, content, thumbnail_url, start_date, end_date, view_count, created_at)
VALUES (
    '[모집] 한스푼 맛칼럼니스트 & 공식 서포터즈 1기 모집!', 
    '<h3>📝 한스푼 맛칼럼니스트 & 공식 서포터즈 1기 모집</h3><img src="/img/event-supporters.jpg" alt="event illustration" /><p>요리에 진심인 분들을 찾습니다! 한스푼과 함께 요리 문화를 만들어갈 <strong>공식 서포터즈 1기</strong>에 지원하세요.</p><ul><li><strong>활동 혜택:</strong> 매월 5만 스푼 지급, 굿즈 제공</li><li><strong>지원 방법:</strong> 마이페이지 하단 신청 링크 작성</li></ul>',
    'https://images.unsplash.com/photo-1466637574441-749b8f19452f?auto=format&fit=crop&q=80&w=500',
    DATE_SUB(NOW(), INTERVAL 2 DAY), 
    DATE_ADD(NOW(), INTERVAL 14 DAY), 
    0, NOW()
);

-- 3. 종료된 이벤트
INSERT INTO event (title, content, thumbnail_url, start_date, end_date, view_count, created_at)
VALUES (
    '[종료] 2025 연말 요리 레시피 공모전', 
    '<h3>🏆 2025 연말 요리 레시피 공모전 당첨자 발표</h3><p>성원에 감사드립니다. 본 공모전은 종료되었습니다. 당첨되신 분들께는 개별 연락을 드렸습니다.</p><p>내년에도 더 풍성한 이벤트로 찾아뵙겠습니다!</p>',
    'https://images.unsplash.com/photo-1514326640560-7d063ef2aed5?auto=format&fit=crop&q=80&w=500',
    DATE_SUB(NOW(), INTERVAL 60 DAY), 
    DATE_SUB(NOW(), INTERVAL 30 DAY), 
    0, NOW()
);

-- 4. 진행 중인 이벤트 (봄맞이 레시피 공모전)
INSERT INTO event (title, content, thumbnail_url, start_date, end_date, view_count, created_at)
VALUES (
    '[이벤트] 봄 향기 가득! 제철 나물 레시피 공모전', 
    '<h3>🌱 제철 나물 레시피 공모전</h3><img src="https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&q=80&w=1000" alt="spring food" /><p>따스한 봄을 맞아 여러분만의 특별한 나물 요리 레시피를 공유해 주세요!</p><ul><li><strong>기간:</strong> 3월 한 달간</li><li><strong>우수작 발표:</strong> 4월 5일</li><li><strong>경품:</strong> 주방 도구 세트 & 5만 스푼</li></ul>',
    'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&q=80&w=500',
    DATE_SUB(NOW(), INTERVAL 1 DAY), 
    DATE_ADD(NOW(), INTERVAL 20 DAY), 
    0, NOW()
);

-- 5. 진행 중인 이벤트 (첫 클래스 할인)
INSERT INTO event (title, content, thumbnail_url, start_date, end_date, view_count, created_at)
VALUES (
    '[할인] 첫 원데이 클래스 예약 시 20% 특별 할인!', 
    '<h3>🍳 배움의 즐거움, 첫 클래스 20% 할인 쿠폰</h3><img src="https://images.unsplash.com/photo-1556910110-a5a63dfd393c?auto=format&fit=crop&q=80&w=1000" alt="cooking class" /><p>한스푼에서 처음으로 요리 클래스를 들어보시는 분들을 위해 <strong>20% 할인 쿠폰</strong>을 드립니다.</p><p>지금 바로 클래스를 둘러보고 혜택을 받으세요!</p>',
    'https://images.unsplash.com/photo-1556910110-a5a63dfd393c?auto=format&fit=crop&q=80&w=500',
    DATE_SUB(NOW(), INTERVAL 10 DAY), 
    DATE_ADD(NOW(), INTERVAL 90 DAY), 
    0, NOW()
);

-- 6. 진행 중인 이벤트 (이달의 리뷰어)
INSERT INTO event (title, content, thumbnail_url, start_date, end_date, view_count, created_at)
VALUES (
    '[안내] 이달의 베스트 리뷰어 선정 이벤트', 
    '<h3>📸 정성스러운 기록, 이달의 베스트 리뷰어</h3><p>직접 만든 요리 사진과 함께 정성껏 리뷰를 남겨주시는 분들 중 매달 5분을 선정하여 특별한 선물을 드립니다.</p><ul><li><strong>선정 기준:</strong> 사진 포함 여부, 유용한 팁 공유 등</li><li><strong>혜택:</strong> 한스푼 프리미엄 밀키트 세트</li></ul>',
    'https://images.unsplash.com/photo-1484723091739-30a097e8f929?auto=format&fit=crop&q=80&w=500',
    DATE_SUB(NOW(), INTERVAL 15 DAY), 
    DATE_ADD(NOW(), INTERVAL 15 DAY), 
    0, NOW()
);

-- 7. 오픈 예정 이벤트 (제휴 이벤트)
INSERT INTO event (title, content, thumbnail_url, start_date, end_date, view_count, created_at)
VALUES (
    '[커밍순] 한스푼 x 유기농 농장 상생 프로젝트 오픈 예정', 
    '<h3>🍎 자연을 담은 식탁, 상생 프로젝트</h3><img src="https://images.unsplash.com/photo-1464226184884-fa280b87c399?auto=format&fit=crop&q=80&w=1000" alt="farm" /><p>전국 산지의 싱싱한 식재료를 직거래로 만나볼 수 있는 새로운 서비스가 곧 시작됩니다!</p><p>기대평을 남겨주시는 분들께는 오픈 알림과 함께 적립금을 드립니다.</p>',
    'https://images.unsplash.com/photo-1464226184884-fa280b87c399?auto=format&fit=crop&q=80&w=500',
    DATE_ADD(NOW(), INTERVAL 7 DAY), 
    DATE_ADD(NOW(), INTERVAL 21 DAY), 
    0, NOW()
);

-- 8. 종료된 이벤트 (발렌타인)
INSERT INTO event (title, content, thumbnail_url, start_date, end_date, view_count, created_at)
VALUES (
    '[종료] 달콤한 마음을 전해요, 수제 초콜릿 클래스', 
    '<h3>💝 발렌타인 데이 수제 초콜릿 클래스</h3><p>초콜릿 클래스 이벤트가 많은 분들의 참여 속에 성황리에 종료되었습니다.</p><p>참여해 주신 모든 분들께 특별한 추억이 되었기를 바랍니다!</p>',
    'https://images.unsplash.com/photo-1548907040-4baa42d10919?auto=format&fit=crop&q=80&w=500',
    DATE_SUB(NOW(), INTERVAL 40 DAY), 
    DATE_SUB(NOW(), INTERVAL 10 DAY), 
    0, NOW()
);

-- 9. 진행 중인 이벤트 (건강 챌린지)
INSERT INTO event (title, content, thumbnail_url, start_date, end_date, view_count, created_at)
VALUES (
    '[챌린지] 일주일 1회 건강한 집밥 해먹기 챌린지', 
    '<h3>🥗 내 몸을 위한 건강한 습관!</h3><img src="https://images.unsplash.com/photo-1490645935967-10de6ba17061?auto=format&fit=crop&q=80&w=1000" alt="healthy food" /><p>가공식품 대신 직접 만든 건강한 한 끼를 인증해 주세요!</p><ul><li><strong>참여 방법:</strong> 레시피 게시판에 #건강챌린지 태그와 함께 글 작성</li><li><strong>혜택:</strong> 챌린지 성공 배지 & 전용 할인권</li></ul>',
    'https://images.unsplash.com/photo-1490645935967-10de6ba17061?auto=format&fit=crop&q=80&w=500',
    DATE_SUB(NOW(), INTERVAL 3 DAY), 
    DATE_ADD(NOW(), INTERVAL 25 DAY), 
    0, NOW()
);
