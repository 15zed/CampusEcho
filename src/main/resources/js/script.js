// 从后端获取朋友圈动态数据的函数
async function fetchFriendCircleData() {
    try {
        const response = await fetch("/api/getdata");
        const infosWithComments = await response.json();
        return infosWithComments; // 返回帖子和评论的集合
    } catch (error) {
        console.error('Error fetching friend circle data:', error);
        return [];
    }
}

// 向后端发送帖子数据的函数
async function sendPostToBackend(info) {
    try {
        const response = await fetch("/api/post", {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(info),
        });
        const data = await response.json();
        return data; // 后端返回的数据包含新创建的帖子信息
    } catch (error) {
        console.error('Error sending post data:', error);
        return null;
    }
}

// 将评论发送到后端的函数
async function sendCommentToBackend(commentInfo) {
    try {
        const response = await fetch("/api/comment", {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(commentInfo),
        });

        const data = await response.json();
        return data; // 后端返回的数据包含新创建的评论信息
    } catch (error) {
        console.error('Error sending comment data:', error);
        return null;
    }
}


// 定义每页显示的帖子数量
const postsPerPage = 8;

// 当前页码
let currentPage = 1;



// 在页面加载完成后执行 获取所有帖子并渲染
window.onload = async function() {
    // 获取已存在的帖子
    const existingPosts = await fetchFriendCircleData();

    // 初始化分页
    initPagination(existingPosts);

    // 显示第一页的帖子数据
    renderPostsByPage(currentPage,existingPosts);
};

// 初始化分页
function initPagination(allPosts) {
    const totalPages = Math.ceil(allPosts.length / postsPerPage);

    // 获取分页容器
    const paginationContainer = document.getElementById("pagination-container");

    //清空原来容器中所有的button
    paginationContainer.innerHTML = '';

    // 创建页码按钮并添加点击事件
    for (let i = 1; i <= totalPages; i++) {
        const pageButton = document.createElement("button");
        pageButton.textContent = i;
        pageButton.addEventListener("click", function() {
            currentPage = i;
            renderPostsByPage(currentPage,allPosts);
            updatePaginationButtons();
        });
        // 只显示当前页及其相邻的两个页码按钮
        if (i === 1 || i === 2 || i === 3) {
            const pageInfoElement = document.getElementById("page-info");
            pageInfoElement.textContent = `当前页: ${currentPage}，共 ${totalPages} 页`;
            pageButton.style.display = "inline-block";
        } else {
            pageButton.style.display = "none";
        }
        paginationContainer.appendChild(pageButton);
    }
}

// 根据当前页码显示帖子数据
async function renderPostsByPage(page,allPosts) {
    // const allPosts = await fetchFriendCircleData();

    const startIndex = (page - 1) * postsPerPage;
    const endIndex = startIndex + postsPerPage;
    const postsToDisplay = allPosts.slice(startIndex, endIndex);

    // 清空之前的帖子数据
    const postsList = document.getElementById("posts-list");
    postsList.innerHTML = '';

    // 渲染当前页的帖子数据
    renderPosts(postsToDisplay);
}

// 更新页码按钮状态
function updatePaginationButtons() {
    const paginationContainer = document.getElementById("pagination-container");
    const paginationButtons = paginationContainer.querySelectorAll("button");


    // 遍历按钮，显示当前页及附近的页码按钮
    paginationButtons.forEach(button => {
        const pageNumber = parseInt(button.textContent);

        if (pageNumber === currentPage || pageNumber === currentPage - 1 || pageNumber === currentPage + 1) {
            button.style.display = "inline-block";
        } else {
            button.style.display = "none";
        }
    });

    // 更新显示当前页和总页数的元素
    // const pageInfoElement = document.getElementById("page-info");
    // pageInfoElement.textContent = `当前页: ${currentPage}，共 ${paginationButtons.length} 页`;

    updatePageInfo(currentPage,paginationButtons.length);
}

// 渲染帖子和评论
function renderPosts(infosWithComments) {
    const postsList = document.getElementById("posts-list");

    infosWithComments.forEach(({ info, comments }) => {
        const postElement = createPostElement(info);
        const commentsContainer = document.createElement("div");

        // 渲染评论
        comments.forEach(comment => {
            const commentElement = createCommentElement(comment);
            commentsContainer.appendChild(commentElement);
        });

        // 将评论容器添加到帖子元素中
        postElement.appendChild(commentsContainer);

        postsList.appendChild(postElement);
    });
}


// 去用户的主页
function redirectToUserPage(userId) {
    const userPageUrl = `/user/${userId}`;
    window.location.href = userPageUrl;
}


// 创建帖子元素
function createPostElement(info) {
    const postElement = document.createElement("div");
    postElement.id = `post-${info.id}`; // 设置帖子元素的ID
    postElement.className = "post";

    let avatarElement = null;
    // 展示用户头像
    if (info.avatar != null && info.avatar !== "") {
        avatarElement = document.createElement("img");
        avatarElement.className = "avatar";
        avatarElement.src = "/get/" + info.avatar;
        postElement.appendChild(avatarElement);
    }
    // 展示文本内容
    if (info.text != null && info.text !== "") {
        const textElement = document.createElement("p");
        textElement.innerHTML = info.text;
        postElement.appendChild(textElement);
    }
    // 展示图片
    if (info.img != null && info.img.length > 0) {
        info.img.forEach(imgUrl => {
            const imageElement = document.createElement("img");
            imageElement.src = "/get/" + imgUrl;
            postElement.appendChild(imageElement);
        });
    }

    // 展示发帖时间
    const timeElement = document.createElement("p");
    timeElement.textContent = info.time;
    postElement.appendChild(timeElement);

    // 添加评论容器
    const commentsContainer = document.createElement("div");
    commentsContainer.id = `comments-container-${info.id}`; // 设置评论容器的ID
    postElement.appendChild(commentsContainer);

    // 添加评论按钮
    const commentButton = document.createElement("button");
    commentButton.textContent = "评论";
    commentButton.onclick = function () {
        showCommentInput(info.id); // 传入帖子ID
    };
    // postElement.appendChild(commentButton);
    postElement.insertBefore(commentButton,commentsContainer);

    // 添加点赞按钮
    const likeButton = document.createElement("button");
    likeButton.textContent = "点赞";
    likeButton.onclick = function () {
        toggleLike(info.id); // 切换点赞状态
    };
    // postElement.appendChild(likeButton);
    postElement.insertBefore(likeButton,commentsContainer);

    // 添加点赞数量显示
    const likeCountElement = document.createElement("span");
    likeCountElement.className = "likes-count";
    likeCountElement.textContent = `点赞数: ${info.likes}`;
    postElement.insertBefore(likeCountElement,commentsContainer);

    // 添加评论输入框
    const commentInput = document.createElement("textarea");
    commentInput.placeholder = "发表评论...";
    commentInput.style.display = "none";
    commentInput.id = `comment-input-${info.id}`; // 设置评论输入框的ID
    postElement.appendChild(commentInput);

    // 添加发表评论按钮
    const postCommentButton = document.createElement("button");
    postCommentButton.textContent = "发表评论";
    postCommentButton.style.display = "none";
    postCommentButton.id = `post-comment-button-${info.id}`; // 设置发表评论按钮的ID
    postCommentButton.onclick = function () {
        if(commentInput.value == null || commentInput.value.trim() === "") {
            alert("评论不能为空！");
            return;
        }
        createComment(info.id, commentInput.value); // 传入帖子ID和评论内容
    };
    postElement.appendChild(postCommentButton);



    if (avatarElement != null) {
        //添加点击头像进入主页的功能
        avatarElement.onclick = function () {
            redirectToUserPage(info.userId);
        };
    }

    return postElement;
}

// 显示评论输入框
function showCommentInput(id) {

    const commentInput = document.querySelector(`#comment-input-${id}`);
    const postCommentButton = document.querySelector(`#post-comment-button-${id}`);

    if (commentInput.style.display === "none") {
        commentInput.style.display = "block";
        postCommentButton.style.display = "block";
    } else {
        commentInput.style.display = "none";
        postCommentButton.style.display = "none";
    }
}

// 获取当前时间
function getFormattedTime() {
    //生成当前时间
    const now = new Date();
    const year = now.getFullYear();
    const month = ('0' + (now.getMonth() + 1)).slice(-2);
    const day = ('0' + now.getDate()).slice(-2);
    const hours = ('0' + now.getHours()).slice(-2);
    const minutes = ('0' + now.getMinutes()).slice(-2);
    const seconds = ('0' + now.getSeconds()).slice(-2);
    //拼接为String
    const formattedTime = year + "-" + month + "-" + day +" "+ hours + ":" + minutes + ":" + seconds;
    return formattedTime;
}

// 发表评论
async function createComment(id, commentText) {
    const jsonUser = document.getElementById("user").innerText;
    const user = JSON.parse(jsonUser);

    const formattedTime = getFormattedTime(); // 你之前使用的获取时间的函数

    const commentInfo = {
        userId: user.userId,
        pubId: id,
        text: commentText,
        time: formattedTime,
    };

    // 向后端发送评论数据
    const newCommentData = await sendCommentToBackend(commentInfo);

    // 渲染新创建的评论
    if (newCommentData) {
        const commentsContainer = document.querySelector(`#comments-container-${id}`);
        const newCommentElement = createCommentElement(newCommentData);
        commentsContainer.appendChild(newCommentElement);
    }

    // 隐藏评论输入框
    const commentInput = document.querySelector(`#comment-input-${id}`);
    const postCommentButton = document.querySelector(`#post-comment-button-${id}`);
    commentInput.style.display = "none";
    postCommentButton.style.display = "none";
}

// 创建评论元素
function createCommentElement(commentInfo) {
    const commentElement = document.createElement("div");
    commentElement.className = "comment";

    // 添加评论者头像等信息
    // 这里你可以根据评论信息创建合适的DOM结构

    // 添加评论文本
    const textElement = document.createElement("p");
    textElement.innerHTML = commentInfo.text;
    commentElement.appendChild(textElement);

    // 添加评论时间
    const timeElement = document.createElement("p");
    timeElement.textContent = commentInfo.time;
    commentElement.appendChild(timeElement);

    return commentElement;
}

// 点赞函数
async function toggleLike(id) {
    try {
        const response = await fetch(`/api/${id}/like`, {
            method: 'POST',
        });

        if (response.ok) {
            const updatedPostData = await response.json();
            updateLikesCountOnPage(id, updatedPostData.likes); // 只更新点赞数,不重新渲染整个帖子
        } else {
            console.error('Failed to toggle like.');
        }
    } catch (error) {
        console.error('Error toggling like:', error);
    }
}

// 更新点赞数的函数
function updateLikesCountOnPage(id, likesCount) {
    const postElement = document.getElementById(`post-${id}`);
    if (postElement) {
        // 找到点赞数量的元素
        const likeCountElement = postElement.querySelector('.likes-count');

        if (likeCountElement) {
            likeCountElement.textContent = `点赞数: ${likesCount}`; // 更新点赞数显示
        }
    }
}


//回到登录页
function getBackToLogin(){
    window.location.href = '/user/forward';
}



// 显示发表帖子的输入框
function showPostInput() {
    const postInput = document.getElementById("post-input");
    postInput.style.display = "block";
}

//去我的主页
function toMyHome(){
    const jsonUser = document.getElementById("user").innerText;
    const user = JSON.parse(jsonUser);
    redirectToUserPage(user.userId);
}

// 发表帖子
async function createPost() {
    const text = document.getElementById("post-text").value;
    const imageFiles = document.getElementById("post-image").files;

    const category = document.getElementById("category").value;

    // 获取用户头像路径（假设在登录时已经获取到）
    const jsonUser = document.getElementById("user").innerText;

    const user = JSON.parse(jsonUser);
    const userAvatarUrl = user.head; // 替换成实际的用户头像路径

    const formattedTime = getFormattedTime();

    // 最大允许上传图片的数量
    const maxImageCount = 6;

    // 检查图片数量是否超过限制
    if (imageFiles.length > maxImageCount) {
        alert(`最多只能上传${maxImageCount}张图片`);
        return;
    }

    const info = {
        userId: user.userId, // 替换成实际的用户ID
        avatar: userAvatarUrl,
        text: text,
        // 其他帖子数据...
        time: formattedTime,
        likes: 0,
        category: category
    };

// 如果有上传的图片，将图片上传并获取图片的URL
    if (imageFiles.length > 0) {
        const imageUrls = [];

        for (let i = 0; i < Math.min(imageFiles.length, maxImageCount); i++) {
            const imageUrl = await uploadImageToBackend(imageFiles[i]);
            imageUrls.push(imageUrl);
        }
        info.img = imageUrls;
    }

    // 向后端发送帖子数据
    const newPostData = await sendPostToBackend(info);

    // 渲染新创建的帖子
    if (newPostData) {
        const postsList = document.getElementById("posts-list");
        const newPostElement = createPostElement(newPostData);
        postsList.insertBefore(newPostElement, postsList.firstChild); // 插入到列表的最前面
    }

    // 隐藏发表帖子的输入框
    const postInput = document.getElementById("post-input");
    postInput.style.display = "none";
}

// 将帖子图片上传到后端的函数
async function uploadImageToBackend(image) {
    try {
        const formData = new FormData();
        formData.append('image', image);

        const response = await fetch("/api/uploadImage", {
            method: 'POST',
            body: formData,
        });

        const data = await response.json();
        return data; // 假设后端返回的数据包含图片的URL
    } catch (error) {
        console.error('Error uploading image:', error);
        return null;
    }
}

// 从后端获取热门帖子数据的函数
async function fetchHotPosts() {
    try {
        const response = await fetch("/api/hotPosts");
        const hotPostsData = await response.json();
        return hotPostsData;
    } catch (error) {
        console.error('Error fetching hot posts data:', error);
        return [];
    }
}

// 更新页面元素，显示当前页和总页数
function updatePageInfo(currentPage, totalPages) {
    const pageInfoElement = document.getElementById("page-info");
    pageInfoElement.textContent = `当前页: ${currentPage}，共 ${totalPages} 页`;
}

// 热门帖子按钮点击事件
async function showHotPosts() {
    try {
        // 获取热门帖子数据
        const hotPostsData = await fetchHotPosts();

        //初始化分页
        initPagination(hotPostsData);

        // 显示第一页的帖子数据
        renderPostsByPage(currentPage,hotPostsData);

        // 渲染热门帖子数据
        // renderPosts(hotPostsData);

        // // 计算并更新总页数
        // const totalPages = Math.ceil(hotPostsData.length / postsPerPage);
        //
        // // 更新页面元素，保留分页控件的显示，并显示当前页和总页数
        // updatePageInfo(currentPage, totalPages);

        // updatePaginationButtons()


    } catch (error) {
        console.error('Error showing hot posts:', error);
    }
}

//查询帖子
async function searchPost() {
    const keyword = document.getElementById("search").value;
    try {
        // 获取搜索到的帖子数据
        const response = await fetch("/api/search/" + keyword);
        const data = await response.json();



        // 检查数据是否为空
        if (data && data.length > 0) {
            // 初始化分页
            initPagination(data);
            // 显示第一页的帖子数据
            renderPostsByPage(currentPage, data);

            // 检查容器中是否已经存在返回按钮
            const backButtonExists = document.getElementById("back-button");

            // 创建返回按钮
            if (!backButtonExists) {
                const backButton = document.createElement("button");
                backButton.textContent = "返回";
                backButton.id = "back-button"; // 设置按钮的 id，用于检查存在性
                backButton.addEventListener("click", function () {
                    window.location.href = "/api/welcome";
                });
                const searchButton = document.getElementById("search-button");
                searchButton.after(backButton);
            }

        } else {
            // 获取容器
            const paginationContainerAndInfo = document.getElementById("pagination-container-and-info");
            //清空原来容器中所有的元素
            paginationContainerAndInfo.innerHTML = '';
            // 数据为空，显示提示信息
            showNoRecordsMessage();
            //返回主页面
            setTimeout(function (){
                window.location.href = "/api/welcome";
            },2000);

        }
    } catch (error) {
        console.error('Error sending post data:', error);
        return null;
    }
}

//查询没有数据时的处理
function showNoRecordsMessage() {
    // 根据需要显示提示信息，例如更新页面中的特定元素
    const postsListElement = document.getElementById("posts-list");
    if (postsListElement) {
        postsListElement.innerHTML = "<p>找到0条记录</p>";
    }



}

// 每日推荐按钮点击事件
async function dailyRecommendations() {
    try {
        // 获取推荐帖子数据
        const recommendPostsData = await fetchRecommendationPosts();

        //初始化分页
        initPagination(recommendPostsData);

        // 显示第一页的帖子数据
        renderPostsByPage(currentPage,recommendPostsData);

        // 渲染热门帖子数据
        // renderPosts(hotPostsData);

        // // 计算并更新总页数
        // const totalPages = Math.ceil(hotPostsData.length / postsPerPage);
        //
        // // 更新页面元素，保留分页控件的显示，并显示当前页和总页数
        // updatePageInfo(currentPage, totalPages);

        // updatePaginationButtons()


    } catch (error) {
        console.error('Error showing hot posts:', error);
    }
}

// 从后端获取热门帖子数据的函数
async function fetchRecommendationPosts() {
    try {
        const response = await fetch("/api/recommendPosts");
        const recommendPostsData = await response.json();
        return recommendPostsData;
    } catch (error) {
        console.error('Error fetching hot posts data:', error);
        return [];
    }
}



