const filterForm = document.getElementById("filter-form");
const statsText = document.getElementById("stats");
const message = document.getElementById("message");
const reportList = document.getElementById("report-list");

const clearFiltersButton = document.getElementById("clear-filters");
const areaFilter = document.getElementById("area");
const statusFilter = document.getElementById("status");
const categoryFilter = document.getElementById("category");
const sortFilter = document.getElementById("sort");

function createReportItem(report) {
    const status = document.createElement("span");
    status.className = badgeClass(report.status);
    status.textContent = formatLabel(report.status);

    const category = document.createElement("span");
    category.className = "tag";
    category.textContent = formatLabel(report.category);

    const badges = document.createElement("div");
    badges.className = "card-badges";
    badges.append(status, category);

    const link = document.createElement("a");
    link.href = "report.html?id=" + report.id;
    link.textContent = report.title;

    const title = document.createElement("h2");
    title.className = "card-title";
    title.append(link);

    const place = document.createElement("p");
    place.className = "card-place";
    place.textContent = report.address
        ? report.area + " - " + report.address
        : report.area;

    const votes = document.createElement("p");
    votes.className = "votes";
    votes.textContent = formatVotes(report.upvoteCount);

    const item = document.createElement("li");
    item.className = "card report-card";
    item.append(badges, title, place, votes);

    return item;
}

function showStats(stats) {
    const parts = Object.entries(stats.byStatus)
        .map(([status, count]) => formatLabel(status) + ": " + count);

    statsText.textContent = parts.join(" | ");
}

function hasActiveFilters() {
    return Boolean(
        areaFilter.value.trim()
        || statusFilter.value
        || categoryFilter.value
        || sortFilter.value !== "newest"
    );
}

function updateClearFiltersVisibility() {
    clearFiltersButton.hidden = !hasActiveFilters();
}

function hasAnyReports(stats) {
    return Object.values(stats.byStatus)
        .some(count => count > 0);
}

async function loadReports() {
    updateClearFiltersVisibility();

    message.className = "message";
    message.setAttribute("role", "status");
    message.setAttribute("aria-live", "polite");
    message.textContent = "Loading reports...";

    reportList.replaceChildren();

    try {
        const filters = Object.fromEntries(
            new FormData(filterForm)
        );

        const [reports, stats] = await Promise.all([
            getReports(filters),
            getStats()
        ]);

        showStats(stats);

        if (!hasAnyReports(stats)) {
            message.textContent =
                "There are no reports yet. Be the first to report a problem in your city.";
        } else if (reports.length === 0 && hasActiveFilters()) {
            message.textContent =
                "No reports match these filters. Try changing your filters or clear them to see all reports.";
        } else if (reports.length === 0) {
            message.textContent = "There are no reports to show.";
        } else {
            message.textContent = "";
        }

        reports.forEach(report => {
            reportList.append(createReportItem(report));
        });
    } catch (error) {
        message.className = "message message-error";
        message.textContent =
            "Could not load reports: " + error.message;
    }
}

filterForm.addEventListener("submit", event => {
    event.preventDefault();
    loadReports();
});

clearFiltersButton.addEventListener("click", () => {
    areaFilter.value = "";
    statusFilter.value = "";
    categoryFilter.value = "";
    sortFilter.value = "newest";

    updateClearFiltersVisibility();
    loadReports();
});

[areaFilter, statusFilter, categoryFilter, sortFilter]
    .forEach(filter => {
        filter.addEventListener("input", updateClearFiltersVisibility);
        filter.addEventListener("change", updateClearFiltersVisibility);
    });

updateClearFiltersVisibility();
loadReports();